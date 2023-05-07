package dev.naturecodevoid.voicechatdiscord.audio;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dev.naturecodevoid.voicechatdiscord.Common.api;


// Helper for Discord bots to queue and poll audio streams.
public class AudioBridge {

    // === OUTGOING ===

    // Outgoing audio from player microphone sound sources.
    public record OutgoingAudioStream(OpusDecoder decoder, Queue<Short> queue) {}
    private final HashMap<UUID, OutgoingAudioStream> outgoingAudio = new HashMap<>();

    public synchronized OpusDecoder getOutgoingDecoder(UUID source) {
        OutgoingAudioStream stream = this.outgoingAudio.getOrDefault(source, null);
        if (stream == null) {
            stream = new OutgoingAudioStream(api.createDecoder(), new ConcurrentLinkedQueue<>());
            this.outgoingAudio.put(source, stream);
        }
        return stream.decoder();
    }

    // Add sound that will play in parallel to all microphone
    // audio sources from different UUIDs, but in series to
    // sources from same UUID.
    // (That was added since the last poll)
    public synchronized void addOutgoingAudio(UUID source, short[] audio) {
        OutgoingAudioStream stream = this.outgoingAudio.getOrDefault(source, null);
        if (stream == null) {
            stream = new OutgoingAudioStream(api.createDecoder(), new ConcurrentLinkedQueue<>());
            this.outgoingAudio.put(source, stream);
        }
        if (audio.length == 0) {
            stream.decoder().resetState();
        }
        Queue<Short> queue = stream.queue();
        for (short data : audio) {queue.add(data);}
    }

    // Returns whether generic or microphone audio was added.
    // (Since the last poll)
    public synchronized boolean hasOutgoingAudio() {
        return this.outgoingAudio.values().stream().anyMatch(stream -> ! stream.queue().isEmpty());
    }

    // Returns 20ms of combined generic and microphone audio
    // and removes what was used.
    public synchronized short[] pollOutgoingAudio() {
        List<List<Short>> audioParts = new LinkedList<>();

        this.outgoingAudio.values().forEach(stream -> {
            List<Short> audioPart = new LinkedList<>();
            Queue<Short> queue = stream.queue();
            for (int i=0;i< AudioCore.COUNT20MS ;i++) {
                if (queue.isEmpty()) {break;}
                audioPart.add(queue.poll());
            }
            if (! audioPart.isEmpty()) {
                audioParts.add(audioPart);
            }
        });
        this.outgoingAudio.keySet().removeIf(key -> {
            OutgoingAudioStream stream = this.outgoingAudio.get(key);
            if (stream.queue().isEmpty()) {
                stream.decoder().close();
                return true;
            }
            return false;
        });

        return AudioCore.combineAudioParts(audioParts);
    }


    // === INCOMING ===

    // Incoming audio from the Discord user's microphone.
    private final Queue<Short> incomingAudio = new ConcurrentLinkedQueue<>();

    // Add sound that will play in series to previously
    // added incoming audio.
    // (That was added since the last poll)
    public synchronized void addIncomingMicrophoneAudio(short[] audio) {
        for (short data : audio) {this.incomingAudio.add(data);}
    }

    // Returns 20ms of microphone audio and removes what was used.
    public synchronized short[] pollIncomingAudio() {
        short[] audio = new short[AudioCore.COUNT20MS];
        for (int i=0;i< AudioCore.COUNT20MS ;i++) {
            if (this.incomingAudio.isEmpty()) {break;}
            audio[i] = this.incomingAudio.poll();
        }
        return audio;
    }


    // === COMMON ===

    // Removes all queued outgoing and incoming audio.
    public synchronized void clear() {
        this.outgoingAudio.clear();
        this.incomingAudio.clear();
    }

}
