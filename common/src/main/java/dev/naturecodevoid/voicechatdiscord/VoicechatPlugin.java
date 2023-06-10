package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

import static dev.naturecodevoid.voicechatdiscord.Common.api;
import static dev.naturecodevoid.voicechatdiscord.Common.platform;

public class VoicechatPlugin implements de.maxhenkel.voicechat.api.VoicechatPlugin {
    @Override
    public String getPluginId() {
        return Constants.PLUGIN_ID;
    }

    @Override
    public void initialize(VoicechatApi serverApi) {
        api = (VoicechatServerApi) serverApi;
        platform.info("Successfully initialized Simple Voice Chat plugin");
    }
}
