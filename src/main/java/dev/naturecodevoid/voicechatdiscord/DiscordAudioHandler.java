package dev.naturecodevoid.voicechatdiscord;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dev.naturecodevoid.voicechatdiscord.BukkitPlugin.api;
import static dev.naturecodevoid.voicechatdiscord.BukkitPlugin.encoder;

public class DiscordAudioHandler implements AudioSendHandler, AudioReceiveHandler {
    public final HashMap<UUID, Queue<short[]>> outgoingAudio = new HashMap<>();
    public final Queue<short[]> incomingAudio = new ConcurrentLinkedQueue<>();

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        if (outgoingIsEmpty())
            return null;

        List<short[]> audioParts = new LinkedList<>();

        for (Queue<short[]> queue : outgoingAudio.values())
            if (!queue.isEmpty())
                audioParts.add(queue.poll());

        // https://github.com/DV8FromTheWorld/JDA/blob/11c5bf02a1f4df3372ab68e0ccb4a94d0db368df/src/main/java/net/dv8tion/jda/internal/audio/AudioConnection.java#L529
        int audioLength = audioParts.stream().mapToInt(it -> it.length).max().getAsInt();
        short[] mix = new short[1920];  // 960 PCM samples for each channel
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

        return ByteBuffer.wrap(encoder.encode(mix));
    }

    public short[] provide20MsIncomingAudio() {
        if (incomingAudio.isEmpty())
            return new short[960];
        return incomingAudio.poll();
    }

    @Override
    public void handleCombinedAudio(@NotNull CombinedAudio combinedAudio) {
        incomingAudio.add(api.getAudioConverter().bytesToShorts(stereoToMono(combinedAudio.getAudioData(1.0))));
    }

    private static final int HI = 0;
    private static final int LO = 1;

    // https://stackoverflow.com/questions/16466515/convert-audio-stereo-to-audio-byte
    private byte[] stereoToMono(byte[] stereo) {
        byte[] mono = new byte[stereo.length / 2];

        for (int i = 0; i < mono.length / 2; ++i) {

            int left = (stereo[i * 4 + HI] << 8) | (stereo[i * 4 + LO] & 0xff);
            int right = (stereo[i * 4 + 2 + HI] << 8) | (stereo[i * 4 + 2 + LO] & 0xff);
            int avg = (left + right) / 2;
            mono[i * 2 + HI] = (byte) ((avg >> 8) & 0xff);
            mono[i * 2 + LO] = (byte) (avg & 0xff);
        }

        return mono;
    }

    public boolean outgoingIsEmpty() {
        for (Queue queue : outgoingAudio.values())
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
    public boolean canReceiveCombined() {
        return true;
    }
}
