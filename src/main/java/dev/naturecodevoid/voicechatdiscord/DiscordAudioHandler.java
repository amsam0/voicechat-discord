package dev.naturecodevoid.voicechatdiscord;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.OpusPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class DiscordAudioHandler implements AudioSendHandler, AudioReceiveHandler {
    private final Bot bot;
//    private boolean hasRefreshedEncoder = false;
//    private boolean hasRefreshedDecoder = false;

    public DiscordAudioHandler(Bot bot) {
        this.bot = bot;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        if (outgoingIsEmpty()) {
//            if (!hasRefreshedEncoder) {
//                bot.discordEncoder.close();
//                bot.discordEncoder = api.createEncoder();
//                hasRefreshedEncoder = true;
//            }
            return null;
        }

//        hasRefreshedEncoder = false;

        List<short[]> audioParts = new LinkedList<>();

        for (Queue<short[]> queue : bot.outgoingAudio.values())
            if (!queue.isEmpty())
                audioParts.add(queue.poll());

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

        return ByteBuffer.wrap(bot.discordEncoder.encode(mix));
    }

    public short[] provide20MsIncomingAudio() {
        if (bot.incomingAudio.isEmpty()) {
//            if (!hasRefreshedDecoder) {
//                bot.discordDecoder.close();
//                bot.discordDecoder = api.createDecoder();
//                hasRefreshedDecoder = true;
//            }
            return new short[960];
        }

//        hasRefreshedDecoder = false;

        return bot.incomingAudio.poll();
    }

    @Override
    public void handleEncodedAudio(@NotNull OpusPacket packet) {
        bot.incomingAudio.add(bot.discordDecoder.decode(packet.getOpusAudio()));
    }

    public boolean outgoingIsEmpty() {
        for (Queue queue : bot.outgoingAudio.values())
            if (!queue.isEmpty())
                return false;

        return true;
    }

    @Override
    public boolean canProvide() {
        return !outgoingIsEmpty();
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    @Override
    public boolean canReceiveEncoded() {
        return true;
    }
}
