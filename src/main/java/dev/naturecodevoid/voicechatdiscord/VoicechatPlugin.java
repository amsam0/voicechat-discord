package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentLinkedQueue;

import static dev.naturecodevoid.voicechatdiscord.BukkitPlugin.*;

public class VoicechatPlugin implements de.maxhenkel.voicechat.api.VoicechatPlugin {
    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        BukkitPlugin.api = (VoicechatServerApi) api;
        BukkitPlugin.decoder = api.createDecoder();
        BukkitPlugin.encoder = api.createEncoder();
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onMicrophonePacket(MicrophonePacketEvent e) {
        if (e.getSenderConnection() == null)
            return;

        if (!(e.getSenderConnection().getPlayer().getPlayer() instanceof Player sender))
            return;

        for (ServerPlayer player : api.getPlayersInRange(
                api.fromServerLevel(sender.getWorld()),
                api.createPosition(
                        sender.getLocation().getX(),
                        sender.getLocation().getY(),
                        sender.getLocation().getZ()
                ),
                api.getVoiceChatDistance()
        )) {
            if (player.getUuid().compareTo(sender.getUniqueId()) == 0 || !connectedPlayers.containsKey(player.getUuid()))
                continue;

            DiscordAudioHandler handler = connectedPlayers.get(player.getUuid()).handler();

            if (!handler.outgoingAudio.containsKey(player.getUuid()))
                handler.outgoingAudio.put(player.getUuid(), new ConcurrentLinkedQueue<>());

            handler.outgoingAudio
                    .get(player.getUuid())
                    .add(
                            adjustVolumeOfOpusEncodedAudio(
                                    e.getPacket().getOpusEncodedData(),
                                    1 - (distance(
                                            player.getPosition(),
                                            e.getSenderConnection().getPlayer().getPosition()
                                    ) / api.getVoiceChatDistance())
                            )
                    );
        }
    }

    private double distance(Position pos1, Position pos2) {
        return Math.sqrt(
                Math.pow(pos1.getX() - pos2.getX(), 2) +
                        Math.pow(pos1.getY() - pos2.getY(), 2) +
                        Math.pow(pos1.getZ() - pos2.getZ(), 2)
        );
    }

    private short[] adjustVolumeOfOpusEncodedAudio(byte[] opusEncodedData, double volume) {
        short[] decoded = decoder.decode(opusEncodedData);
        byte[] decodedAsBytes = api.getAudioConverter().shortsToBytes(decoded);
        byte[] adjustedVolume = adjustVolume(decodedAsBytes, (float) volume);
        return api.getAudioConverter().bytesToShorts(adjustedVolume);
    }

    // https://stackoverflow.com/a/26037576
    private byte[] adjustVolume(byte[] audioSamples, float volume) {
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
