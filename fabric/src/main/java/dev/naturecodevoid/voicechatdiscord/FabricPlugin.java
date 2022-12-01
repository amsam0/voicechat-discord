package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.VoicechatApi;

import static dev.naturecodevoid.voicechatdiscord.VoicechatDiscord.platform;

public class FabricPlugin extends Plugin {
    @Override
    public void initialize(VoicechatApi serverApi) {
        if (platform == null)
            platform = new FabricPlatform();
        super.initialize(serverApi);
    }
}
