package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;

import static dev.naturecodevoid.voicechatdiscord.AudioUtil.addAudioToBotsInRange;
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

        addAudioToBotsInRange(
                e.getSenderConnection().getPlayer(),
                getPlayerDecoder(e.getSenderConnection().getPlayer().getUuid()).decode(e.getPacket().getOpusEncodedData())
        );
    }
}
