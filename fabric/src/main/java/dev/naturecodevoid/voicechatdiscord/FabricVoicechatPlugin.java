package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.VoicechatApi;

import static dev.naturecodevoid.voicechatdiscord.Common.platform;


public class FabricVoicechatPlugin extends VoicechatPlugin {
    @Override
    public void initialize(VoicechatApi serverApi) {
        // sometimes, the voicechat plugin will be initialized before the mod, which makes platform null
        if (platform == null)
            platform = new FabricPlatform();
        super.initialize(serverApi);
    }
}
