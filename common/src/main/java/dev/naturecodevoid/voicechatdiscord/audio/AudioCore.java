package dev.naturecodevoid.voicechatdiscord.audio;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerPlayer;
import dev.naturecodevoid.voicechatdiscord.Bot;
import dev.naturecodevoid.voicechatdiscord.MathUtil;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dev.naturecodevoid.voicechatdiscord.Common.*;

public class AudioCore {

    // Number of shorts that are needed in an array to represent 20ms.
    public static final short COUNT20MS = 960;

    public static short[] combineAudioParts(List<List<Short>> audioParts) {
        // Based on https://github.com/DV8FromTheWorld/JDA/blob/11c5bf02a1f4df3372ab68e0ccb4a94d0db368df/src/main/java/net/dv8tion/jda/internal/audio/AudioConnection.java#L529
        // Slightly modified to take lists instead of arrays, and returns the proper array length, instead of 1920.
        int audioLength = audioParts.stream().mapToInt(List::size).max().getAsInt();
        short[] mix = new short[audioLength]; // 960 PCM samples for each channel
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
    }

    public static short[] adjustVolumeOfOpusDecodedAudio(short[] decoded, double volume) {
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

    public static void addAudioToBotsInRange(Player sender, short[] opusDecodedData) {
        /*Position senderPosition = sender.getPosition();
        UUID senderUuid = sender.getUuid();
        double voiceChatDistance = api.getVoiceChatDistance();

        for (ServerPlayer player : api.getPlayersInRange(
                platform.getServerLevel((ServerPlayer) sender),
                senderPosition,
                voiceChatDistance
        )) {
            if (player.getUuid().compareTo(senderUuid) == 0)
                continue;

            Bot bot = getBotForPlayer(player.getUuid());
            if (bot != null) {
                if (!bot.outgoingAudio.containsKey(senderUuid))
                    bot.outgoingAudio.put(senderUuid, new ConcurrentLinkedQueue<>());

                // I don't know if this is the correct volume formula but it's close enough
                double volume = Math.cos((MathUtil.distance(
                        senderPosition,
                        bot.player.getPosition()
                ) / voiceChatDistance) * (Math.PI / 2));

                bot.outgoingAudio
                        .get(senderUuid)
                        .add(adjustVolumeOfOpusDecodedAudio(opusDecodedData, MathUtil.clamp(volume, 0, 1)));
            }
        }*/
    }
}
