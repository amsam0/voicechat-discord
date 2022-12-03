package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static dev.naturecodevoid.voicechatdiscord.BukkitPlugin.LOGGER;
import static dev.naturecodevoid.voicechatdiscord.VoicechatDiscord.api;

public class BukkitPlatform extends Platform {
    @Override
    public boolean isValidPlayer(Object sender) {
        return sender instanceof Player;
    }

    @Override
    public void sendMessage(Object sender, String message) {
        if (!(sender instanceof CommandSender)) {
            warn("Seems like we are trying to send a message to a sender which is not a CommandSender. Please report this on GitHub issues!");
            return;
        }

        ((CommandSender) sender).sendMessage(message);
    }

    @Override
    public String getName(de.maxhenkel.voicechat.api.Player player) {
        return ((Player) player.getPlayer()).getName();
    }

    @Override
    public ServerLevel getServerLevel(ServerPlayer player) {
        return api.fromServerLevel(((Player) player.getPlayer()).getWorld());
    }

    @Override
    public void sendMessage(de.maxhenkel.voicechat.api.Player player, String message) {
        ((Player) player.getPlayer()).sendMessage(message);
    }

    @Override
    public boolean isValidPlayer(ServerPlayer player) {
        return player.getPlayer() instanceof Player;
    }

    @Override
    public String getConfigPath() {
        return "plugins/voicechat-discord/config.yml";
    }

    @Override
    public void info(String message) {
        LOGGER.info(message);
    }

    @Override
    public void warn(String message) {
        LOGGER.warn(message);
    }

    @Override
    public void error(String message) {
        LOGGER.error(message);
    }
}
