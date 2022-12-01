package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;

import java.util.Iterator;
import java.util.List;

import static dev.naturecodevoid.voicechatdiscord.VoicechatDiscord.api;

public class AudioUtil {
    public static short[] combineAudioParts(List<short[]> audioParts) {
        // https://github.com/DV8FromTheWorld/JDA/blob/11c5bf02a1f4df3372ab68e0ccb4a94d0db368df/src/main/java/net/dv8tion/jda/internal/audio/AudioConnection.java#L529
        int audioLength = audioParts.stream().mapToInt(it -> it.length).max().getAsInt();
        short[] mix = new short[1920]; // 960 PCM samples for each channel
        int sample;
        for (int i = 0; i < audioLength; i++) {
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

    public static short[] adjustVolumeOfOpusEncodedAudio(byte[] opusEncodedData, double volume, OpusDecoder decoder) {
        short[] decoded = decoder.decode(opusEncodedData);
        byte[] decodedAsBytes = api.getAudioConverter().shortsToBytes(decoded);
        byte[] adjustedVolume = adjustVolume(decodedAsBytes, (float) volume);
        return api.getAudioConverter().bytesToShorts(adjustedVolume);
    }

    // this is probably a lot more complicated than it needs to be so feel free to make a PR fixing it
    private static byte[] adjustVolume(byte[] audioSamples, float volume) {
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
