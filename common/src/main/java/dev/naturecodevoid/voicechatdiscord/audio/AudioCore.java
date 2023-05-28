package dev.naturecodevoid.voicechatdiscord.audio;

import de.maxhenkel.voicechat.api.Position;
import dev.naturecodevoid.voicechatdiscord.Util;

import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;

import static dev.naturecodevoid.voicechatdiscord.Common.api;
import static dev.naturecodevoid.voicechatdiscord.Common.platform;

/**
 * Utilities and algorithms for operating on audio streams.
 */
public class AudioCore {
    /**
     * The number of shorts needed for a 20ms packet.
     */
    public static final short SHORTS_IN_20MS = 960;

    /**
     * Combines multiple audio streams into one stream.
     */
    public static short[] combineAudioParts(List<List<Short>> audioParts) {
        // Based on https://github.com/DV8FromTheWorld/JDA/blob/11c5bf02a1f4df3372ab68e0ccb4a94d0db368df/src/main/java/net/dv8tion/jda/internal/audio/AudioConnection.java#L529
        // Slightly modified to take lists instead of arrays, and returns the proper array length instead of 1920.
        OptionalInt audioLengthOpt = audioParts.stream().mapToInt(List::size).max();
        if (audioLengthOpt.isPresent()) {
            int audioLength = audioLengthOpt.getAsInt();
            short[] mix = new short[SHORTS_IN_20MS]; // this will fill the whole array with zeros
            for (int i = 0; i < audioLength; i++) {
                if (i > (SHORTS_IN_20MS - 1)) {
                    platform.error("Audio parts are bigger than 20ms! Some audio may be lost. Please report to GitHub Issues!");
                    break;
                }
                int sample = 0;
                for (Iterator<List<Short>> iterator = audioParts.iterator(); iterator.hasNext(); ) {
                    List<Short> audio = iterator.next();
                    if (i < audio.size())
                        sample += audio.get(i);
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
        platform.debug("no outgoing audio? got " + audioParts.size() + " audio parts");
        return new short[SHORTS_IN_20MS];
    }

    /**
     * Adjusts the volume of an audio stream based on distance.
     */
    public static short[] adjustVolumeBasedOnDistance(short[] decoded, Position sourcePosition, Position targetPosition, double maxDistance) {
        // Hopefully this is a similar volume curve to what Minecraft/OpenAL uses
        double volume = Math.cos((Util.distance(sourcePosition, targetPosition) / maxDistance) * (Math.PI / 2));
        platform.debugExtremelyVerbose("adjusting volume to be " + volume + " (source: " + Util.positionToString(sourcePosition) + "; target: " + Util.positionToString(targetPosition) + ")");
        return adjustVolume(decoded, Util.clamp(volume, 0.0, 1.0));
    }

    /**
     * Converts the decoded audio to bytes, adjusts the volume and converts the audio back to shorts.
     */
    private static short[] adjustVolume(short[] decoded, double volume) {
        byte[] decodedAsBytes = api.getAudioConverter().shortsToBytes(decoded);
        byte[] adjustedVolume = adjustVolume(decodedAsBytes, (float) volume);
        return api.getAudioConverter().bytesToShorts(adjustedVolume);
    }

    private static byte[] adjustVolume(byte[] audioSamples, float volume) {
        // this is probably a lot more complicated than it needs to be so feel free to make a PR fixing it
        // https://stackoverflow.com/a/26037576
        byte[] array = new byte[audioSamples.length];
        for (int i = 0; i < array.length; i += 2) {
            // convert byte pair to int
            short buf1 = audioSamples[i + 1];
            short buf2 = audioSamples[i];

            buf1 = (short) ((buf1 & 0xff) << 8);
            buf2 = (short) (buf2 & 0xff);

            short res = (short) (buf1 | buf2);
            res = (short) (res * volume);

            // convert back
            array[i] = (byte) res;
            array[i + 1] = (byte) (res >> 8);

        }
        return array;
    }
}
