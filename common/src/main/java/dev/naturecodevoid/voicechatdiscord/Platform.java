package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;

public abstract class Platform {
    /**
     * This method should return true if the sender is a org.bukkit.entity.Player or
     * net.minecraft.server.network.ServerPlayerEntity
     *
     * @param sender Should be a CommandSender or net.minecraft.server.network.ServerPlayerEntity
     */
    public abstract boolean isValidPlayer(Object sender);

    /**
     * This method should return true if the player.getPlayer() is a org.bukkit.entity.Player or
     * net.minecraft.entity.player.PlayerEntity
     */
    public abstract boolean isValidPlayer(ServerPlayer player);

    /**
     * This method should return true if the sender is an operator
     *
     * @param sender Should be a CommandSender or net.minecraft.server.network.ServerPlayerEntity
     */
    public abstract boolean isOperator(Object sender);

    /**
     * This method should return true if the sender has the given permission
     *
     * @param sender Should be a CommandSender or net.minecraft.server.network.ServerPlayerEntity
     */
    public abstract boolean hasPermission(Object sender, String permission);

    /**
     * @param sender Should be a CommandSender or net.minecraft.server.network.ServerPlayerEntity
     */
    public abstract void sendMessage(Object sender, String message);

    public abstract void sendMessage(Player player, String message);

    public abstract ServerLevel getServerLevel(ServerPlayer player);

    public abstract String getName(Player player);

    public abstract String getConfigPath();

    // Bukkit uses log4j, fabric uses slf4j
    public abstract void info(String message);

    public abstract void warn(String message);

    public abstract void error(String message);

    public abstract void error(String message, Throwable throwable);
}
