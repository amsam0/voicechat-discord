package dev.naturecodevoid.voicechatdiscord.audio;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dev.naturecodevoid.voicechatdiscord.Common.api;
import static dev.naturecodevoid.voicechatdiscord.Common.platform;

/**
 * Helper for Discord bots to queue and poll audio streams.
 */
public class AudioBridge {
    /**
     * Outgoing audio from player microphone sound sources.
     */
    private final HashMap<UUID, OutgoingAudioStream> outgoingAudio = new HashMap<>();
    /**
     * Incoming audio from the Discord user's microphone.
     */
    private final Queue<Short> incomingAudio = new ConcurrentLinkedQueue<>();

    /**
     * Removes all queued outgoing and incoming audio.
     */
    public void clear() {
        platform.debug("clearing AudioBridge");
        this.outgoingAudio.clear();
        this.incomingAudio.clear();
    }

    // === OUTGOING ===

    public OpusDecoder getOutgoingDecoder(UUID source) {
        platform.debugExtremelyVerbose("getting outgoing decoder for " + source);
        OutgoingAudioStream stream = this.outgoingAudio.getOrDefault(source, null);
        if (stream == null) {
            stream = new OutgoingAudioStream(api.createDecoder(), new ConcurrentLinkedQueue<>());
            this.outgoingAudio.put(source, stream);
        }
        return stream.decoder();
    }

    /**
     * Add sound that will play in parallel to all microphone
     * audio sources from different UUIDs, but in series to
     * sources from same UUID.
     * (That was added since the last poll)
     */
    public void addOutgoingAudio(UUID source, short[] audio) {
        platform.debugExtremelyVerbose("adding outgoing audio for " + source + " (length of audio is " + audio.length + ")");
        OutgoingAudioStream stream = this.outgoingAudio.getOrDefault(source, null);
        if (stream == null) {
            stream = new OutgoingAudioStream(api.createDecoder(), new ConcurrentLinkedQueue<>());
            this.outgoingAudio.put(source, stream);
        }
        if (audio.length == 0) {
            platform.debugVerbose("resetting decoder for " + source);
            stream.decoder().resetState();
        } else {
            Queue<Short> queue = stream.queue();
            for (short data : audio) {
                queue.add(data);
            }
        }
    }

    public boolean hasOutgoingAudio() {
        return this.outgoingAudio.values().stream().anyMatch(stream -> !stream.queue().isEmpty());
    }

    public short[] pollOutgoingAudio() {
        List<List<Short>> audioParts = new LinkedList<>();

        this.outgoingAudio.values().forEach(stream -> {
            List<Short> audioPart = new LinkedList<>();
            Queue<Short> queue = stream.queue();
            for (int i = 0; i < AudioCore.SHORTS_IN_20MS; i++) {
                if (queue.isEmpty()) {
                    platform.debugVerbose("queue is empty, we were able to get " + audioPart.size() + " short");
                    break;
                }
                audioPart.add(queue.poll());
            }
            if (!audioPart.isEmpty()) {
                audioParts.add(audioPart);
            }
        });
        this.outgoingAudio.keySet().removeIf(key -> this.outgoingAudio.get(key).queue().isEmpty());

        platform.debugExtremelyVerbose("combining " + audioParts.size() + " audio parts");
        return AudioCore.combineAudioParts(audioParts);
    }


    // === INCOMING ===

    /**
     * Add sound that will play in series to previously
     * added incoming audio.
     * (That was added since the last poll)
     */
    public void addIncomingMicrophoneAudio(short[] audio) {
        platform.debugExtremelyVerbose("adding " + audio.length + " shorts of incoming audio");
        for (short data : audio) {
            this.incomingAudio.add(data);
        }
    }

    /**
     * Returns 20ms of microphone audio and removes what was used.
     */
    public short[] pollIncomingAudio() {
        short[] audio = new short[AudioCore.SHORTS_IN_20MS];
        for (int i = 0; i < AudioCore.SHORTS_IN_20MS; i++) {
            if (this.incomingAudio.isEmpty()) {
                platform.debugVerbose("incoming audio is empty, we got " + (i + 1) + " shorts of audio from it");
                break;
            }
            audio[i] = this.incomingAudio.poll();
        }
        return audio;
    }

    public record OutgoingAudioStream(OpusDecoder decoder, Queue<Short> queue) {
    }
}
