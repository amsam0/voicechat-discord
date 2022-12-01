package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dev.naturecodevoid.voicechatdiscord.VoicechatDiscord.*;

public class Plugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(VoicechatApi serverApi) {
        api = (VoicechatServerApi) serverApi;
        platform.info("Successfully initialized Simple Voice Chat plugin");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onMicrophonePacket(MicrophonePacketEvent e) {
        if (bots.size() < 1)
            return;

        if (e.getSenderConnection() == null)
            return;

        if (!platform.isValidPlayer(e.getSenderConnection().getPlayer()))
            return;

        Position senderPosition = e.getSenderConnection().getPlayer().getPosition();
        double voiceChatDistance = api.getVoiceChatDistance();

        for (ServerPlayer player : api.getPlayersInRange(
                platform.getServerLevel(e.getSenderConnection().getPlayer()),
                senderPosition,
                voiceChatDistance
        )) {
            Bot bot = getBotForPlayer(player.getUuid());
            if (bot != null) {
                UUID senderUuid = e.getSenderConnection().getPlayer().getUuid();

                if (!bot.outgoingAudio.containsKey(senderUuid))
                    bot.outgoingAudio.put(senderUuid, new ConcurrentLinkedQueue<>());

                // I don't know if this is the correct volume formula but it's close enough
                double volume = Math.cos((distance(
                        senderPosition,
                        bot.player.getPosition()
                ) / voiceChatDistance) * (Math.PI / 2));

                byte[] opusEncodedData = e.getPacket().getOpusEncodedData();
                OpusDecoder decoder = bot.getPlayerDecoder(senderUuid);

                bot.outgoingAudio
                        .get(senderUuid)
                        .add(AudioUtil.adjustVolumeOfOpusEncodedAudio(opusEncodedData, clamp(volume, 0, 1), decoder));
            }
        }
    }

    private double clamp(double val, double min, double max) {
        return Math.min(max, Math.max(min, val));
    }

    private double distance(Position pos1, Position pos2) {
        return Math.sqrt(
                Math.pow(pos1.getX() - pos2.getX(), 2) +
                        Math.pow(pos1.getY() - pos2.getY(), 2) +
                        Math.pow(pos1.getZ() - pos2.getZ(), 2)
        );
    }
}
