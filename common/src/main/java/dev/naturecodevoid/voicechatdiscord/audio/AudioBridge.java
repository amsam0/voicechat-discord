package dev.naturecodevoid.voicechatdiscord.audio;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class AudioBridge {

    // Outgoing

    private final List<Queue<Short>> outgoingGeneric = new ArrayList<>();
    private final HashMap<UUID, Queue<Short>> outgoingMicrophone = new HashMap<>();

    public synchronized void addOutgoingGenericAudio(short[] audio) {
        Queue<Short> queue = new ConcurrentLinkedQueue<>();
        this.outgoingGeneric.add(queue);
        for (short data : audio) {queue.add(data);}
    }

    public synchronized void addOutgoingMicrophoneAudio(UUID player, short[] audio) {
        Queue<Short> queue = this.outgoingMicrophone.getOrDefault(player, null);
        if (queue == null) {
            queue = new ConcurrentLinkedQueue<>();
            this.outgoingMicrophone.put(player, queue);
        }
        for (short data : audio) {queue.add(data);}
    }

    public synchronized boolean hasOutgoingAudio() {
        return this.outgoingMicrophone.values().stream().anyMatch(queue -> ! queue.isEmpty()) || this.outgoingGeneric.stream().anyMatch(queue -> ! queue.isEmpty());
    }

    public synchronized short[] pollOutgoingAudio() {
        List<List<Short>> audioParts = new LinkedList<>();

        this.outgoingGeneric.forEach(queue -> this.pollOutgoingAudioPart(audioParts, queue));
        this.outgoingMicrophone.values().forEach(queue -> this.pollOutgoingAudioPart(audioParts, queue));
        this.outgoingGeneric.removeIf(Collection::isEmpty);
        this.outgoingMicrophone.keySet().stream().filter(player -> this.outgoingMicrophone.get(player).isEmpty()).forEach(this.outgoingMicrophone::remove);

        return AudioCore.combineAudioParts(audioParts);
    }
    private void pollOutgoingAudioPart(List<List<Short>> audioParts, Queue<Short> queue) {
        List<Short> audioPart = new LinkedList<>();
        for (int i=0;i< AudioCore.COUNT20MS ;i++) {
            if (queue.isEmpty()) {break;}
            audioPart.add(queue.poll());
        }
        if (! audioPart.isEmpty()) {
            audioParts.add(audioPart);
        }
    }

    // Incoming

    private final Queue<Short> incomingMicrophone = new ConcurrentLinkedQueue<>();

    public synchronized void addIncomingMicrophoneAudio(short[] audio) {
        for (short data : audio) {this.incomingMicrophone.add(data);}
    }

    public synchronized boolean hasIncomingAudio() {
        return ! this.incomingMicrophone.isEmpty();
    }

    public synchronized short[] pollIncomingAudio() {
        short[] audio = new short[AudioCore.COUNT20MS];
        for (int i=0;i< AudioCore.COUNT20MS ;i++) {
            if (this.incomingMicrophone.isEmpty()) {break;}
            audio[i] = this.incomingMicrophone.poll();
        }
        return audio;
    }

    // Common

    public synchronized void clear() {
        this.outgoingMicrophone.clear();
        this.outgoingGeneric.clear();
    }

}
