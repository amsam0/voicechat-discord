package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static dev.naturecodevoid.voicechatdiscord.Common.api;
import static dev.naturecodevoid.voicechatdiscord.PaperPlugin.LOGGER;

public class PaperPlatform extends Platform {
    @Override
    public boolean isValidPlayer(Object sender) {
        return sender instanceof Player;
    }

    @Override
    public boolean isValidPlayer(ServerPlayer player) {
        return player.getPlayer() instanceof Player;
    }

    @Override
    public boolean isOperator(Object sender) {
        if (!(sender instanceof Player))
            return false;
        return ((Player) sender).isOp();
    }

    @Override
    public boolean hasPermission(Object sender, String permission) {
        if (!(sender instanceof Player))
            return false;
        return ((Player) sender).hasPermission(permission);
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
    public void sendMessage(de.maxhenkel.voicechat.api.Player player, String message) {
        ((Player) player.getPlayer()).sendMessage(message);
    }

    @Override
    public ServerLevel getServerLevel(ServerPlayer player) {
        return api.fromServerLevel(((Player) player.getPlayer()).getWorld());
    }

    @Override
    public String getName(de.maxhenkel.voicechat.api.Player player) {
        return ((Player) player.getPlayer()).getName();
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

    @Override
    public void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
}
