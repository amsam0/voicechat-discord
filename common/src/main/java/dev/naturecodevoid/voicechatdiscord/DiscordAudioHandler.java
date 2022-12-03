package dev.naturecodevoid.voicechatdiscord;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.OpusPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static dev.naturecodevoid.voicechatdiscord.AudioUtil.addAudioToBotsInRange;

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

        return ByteBuffer.wrap(bot.discordEncoder.encode(AudioUtil.combineAudioParts(audioParts)));
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
        short[] audio = bot.discordDecoder.decode(packet.getOpusAudio());
        bot.incomingAudio.add(audio);

        addAudioToBotsInRange(
                bot.player,
                audio
        );
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
