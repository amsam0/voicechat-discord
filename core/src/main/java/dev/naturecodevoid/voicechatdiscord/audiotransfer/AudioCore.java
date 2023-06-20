package dev.naturecodevoid.voicechatdiscord.audiotransfer;

import de.maxhenkel.voicechat.api.Position;
import dev.naturecodevoid.voicechatdiscord.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;

import static dev.naturecodevoid.voicechatdiscord.Core.api;
import static dev.naturecodevoid.voicechatdiscord.Core.platform;

/**
 * Utilities and algorithms for operating on audio streams.
 */
public final class AudioCore {
    /**
     * The number of shorts needed for a 20ms packet.
     */
    public static final short SHORTS_IN_20MS = 960;

    /**
     * Combines multiple audio streams into one stream.
     */
    public static short[] combineAudioParts(List<short[]> audioParts) {
        // Based on https://github.com/DV8FromTheWorld/JDA/blob/11c5bf02a1f4df3372ab68e0ccb4a94d0db368df/src/main/java/net/dv8tion/jda/internal/audio/AudioConnection.java#L529
        OptionalInt audioLengthOpt = audioParts.stream().mapToInt(part -> part.length).max();
        if (audioLengthOpt.isPresent()) {
            int audioLength = audioLengthOpt.getAsInt();
            platform.debugExtremelyVerbose("Max audio length is " + audioLength);
            short[] mix = new short[SHORTS_IN_20MS]; // this will fill the whole array with zeros
            int sample;
            for (int i = 0; i < audioLength; i++) {
                if (i > (SHORTS_IN_20MS - 1)) {
                    platform.error("Audio parts are bigger than 20ms! Some audio may be lost. Please report to GitHub Issues!");
                    break;
                }
                sample = 0;
                for (Iterator<short[]> iterator = audioParts.iterator(); iterator.hasNext(); ) {
                    short[] audio = iterator.next();
                    if (i < audio.length)
                        sample += audio[i];
                    else
                        iterator.remove();
                }
                if (sample > Short.MAX_VALUE)
                    mix[i] = Short.MAX_VALUE;
                else if (sample < Short.MIN_VALUE)
                    mix[i] = Short.MIN_VALUE;
                else
                    mix[i] = (short) sample;
            }
            return mix;
        }
        // Should never be triggered since we don't actually call this function if there is no outgoing audio
        platform.warn("no outgoing audio? Please report to GitHub Issues!");
        return new short[SHORTS_IN_20MS];
    }

    /**
     * Adjusts the volume of an audio stream based on distance.
     */
    public static short @Nullable [] adjustVolumeBasedOnDistance(short[] decoded, Position sourcePosition, Position targetPosition, double maxDistance, boolean whispering) {
        // Hopefully this is a similar volume curve to what Minecraft/OpenAL uses
        double volume = Math.cos((Util.distance(sourcePosition, targetPosition) / maxDistance) * (Math.PI / 2));
        if (whispering) {
            platform.debugExtremelyVerbose("player is whispering, original volume is " + volume);
            volume *= api.getServerConfig().getDouble("whisper_distance_multiplier", 1);
        }
        if (volume <= 0) {
            platform.debugExtremelyVerbose("skipping packet, volume is " + volume + " (source: " + Util.positionToString(sourcePosition) + "; target: " + Util.positionToString(targetPosition) + ")");
            return null;
        }
        platform.debugExtremelyVerbose("adjusting volume to be " + volume + " (source: " + Util.positionToString(sourcePosition) + "; target: " + Util.positionToString(targetPosition) + ")");
        return adjustVolume(decoded, Util.clamp(volume, 0.0, 1.0));
    }

    /**
     * Converts the decoded audio to bytes, adjusts the volume and converts the audio back to shorts.
     */
    private static short[] adjustVolume(short[] decoded, double volume) {
        for (int i = 0; i < decoded.length; i++) {
            long res = Math.round(decoded[i] * volume);
            if (res > Short.MAX_VALUE)
                decoded[i] = Short.MAX_VALUE;
            else if (res < Short.MIN_VALUE)
                decoded[i] = Short.MIN_VALUE;
            else
                decoded[i] = (short) res;
        }
        return decoded;
    }
}
