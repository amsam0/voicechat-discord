package dev.naturecodevoid.voicechatdiscord.audio;

import de.maxhenkel.voicechat.api.Position;
import dev.naturecodevoid.voicechatdiscord.MathUtil;

import java.util.*;

import static dev.naturecodevoid.voicechatdiscord.Common.*;


// Utilities and algorithms for operating on audio streams.
public final class AudioCore {

    private AudioCore() {throw new UnsupportedOperationException(this.getClass().getSimpleName() + " should not be instantiated.");}


    // Number of shorts that are needed in an array to represent 20ms.
    public static final short COUNT20MS = 960;

    // Combines multiple audio streams into one stream.
    public static short[] combineAudioParts(List<List<Short>> audioParts) {
        // Based on https://github.com/DV8FromTheWorld/JDA/blob/11c5bf02a1f4df3372ab68e0ccb4a94d0db368df/src/main/java/net/dv8tion/jda/internal/audio/AudioConnection.java#L529
        // Slightly modified to take lists instead of arrays, and returns the proper array length instead of 1920.
        OptionalInt audioLengthOpt = audioParts.stream().mapToInt(List::size).max();
        if (audioLengthOpt.isPresent()) {
            int audioLength = audioLengthOpt.getAsInt();
            short[] mix = new short[audioLength];
            int sample;
            for (int i = 0; i < audioLength; i++) {
                sample = 0;
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
        } else {
            return new short[0];
        }
    }

    // Adjusts the volume of an audio stream based on distance.
    public static short[] adjustVolumeOfOpusDecodedAudio(short[] audio, Position sourcePosition, Position targetPosition, double maxDistance) {
        double volume = Math.cos((MathUtil.distance(sourcePosition, targetPosition) / maxDistance) * (Math.PI / 2));
        return adjustVolumeOfOpusDecodedAudio(audio, MathUtil.clamp(volume, 0.0, 1.0));
    }
    // Adjusts the volume of an audio stream.
    public static short[] adjustVolumeOfOpusDecodedAudio(short[] audio, double volume) {
        byte[] decodedAsBytes = api.getAudioConverter().shortsToBytes(audio);
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
