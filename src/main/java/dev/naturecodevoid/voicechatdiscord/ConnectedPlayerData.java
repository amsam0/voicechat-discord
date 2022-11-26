package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;

public record ConnectedPlayerData(EntityAudioChannel channel, DiscordAudioHandler handler) {
}
