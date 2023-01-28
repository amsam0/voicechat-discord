package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static dev.naturecodevoid.voicechatdiscord.Common.api;
import static dev.naturecodevoid.voicechatdiscord.FabricMod.LOGGER;

public class FabricPlatform extends Platform {
    @Override
    public boolean isValidPlayer(Object sender) {
        return sender != null;
    }

    @Override
    public boolean isValidPlayer(ServerPlayer player) {
        return player.getPlayer() instanceof PlayerEntity;
    }

    @Override
    public boolean isOperator(Object sender) {
        if (!(sender instanceof ServerPlayerEntity))
            return false;

        return ((ServerPlayerEntity) sender).hasPermissionLevel(2);
    }

    @Override
    public boolean hasPermission(Object sender, String permission) {
        // fabric doesn't have a permission system
        // maybe use LuckPerms API or something
        return false;
    }

    @Override
    public void sendMessage(Object sender, String message) {
        if (!(sender instanceof ServerPlayerEntity)) {
            warn("Seems like we are trying to send a message to a sender which is not a ServerPlayerEntity. Please report this on GitHub issues!");
            return;
        }

        ((ServerPlayerEntity) sender).sendMessage(Text.of(message));
    }

    @Override
    public void sendMessage(Player player, String message) {
        ((PlayerEntity) player.getPlayer()).sendMessage(Text.of(message));
    }

    @Override
    public ServerLevel getServerLevel(ServerPlayer player) {
        return api.fromServerLevel(((PlayerEntity) player.getPlayer()).getWorld());
    }

    @Override
    public String getName(Player player) {
        return ((PlayerEntity) player.getPlayer()).getName().getString();
    }

    @Override
    public String getConfigPath() {
        return "config/voicechat-discord.yml";
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
