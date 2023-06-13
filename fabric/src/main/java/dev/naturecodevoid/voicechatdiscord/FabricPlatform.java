package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static dev.naturecodevoid.voicechatdiscord.Constants.REPLACE_LEGACY_FORMATTING_CODES;
import static dev.naturecodevoid.voicechatdiscord.Core.*;
import static dev.naturecodevoid.voicechatdiscord.FabricMod.LOGGER;

public class FabricPlatform extends Platform {
    @Override
    public boolean isValidPlayer(Object sender) {
        if (sender instanceof CommandContext<?> source)
            return ((ServerCommandSource) source.getSource()).getPlayer() != null;
        return sender != null;
    }

    public ServerPlayer commandContextToPlayer(CommandContext<?> context) {
        return api.fromServerPlayer(((ServerCommandSource) context.getSource()).getPlayer());
    }

    @Override
    public CompletableFuture<@Nullable Position> getEntityPosition(ServerLevel level, UUID uuid) {
        ServerWorld world = (ServerWorld) level.getServerLevel();
        Entity entity = world.getEntity(uuid);
        return CompletableFuture.completedFuture(entity != null ?
                api.createPosition(
                        entity.getX(),
                        entity.getY(),
                        entity.getZ()
                )
                : null);
    }

    @Override
    public boolean isOperator(Object sender) {
        if (sender instanceof CommandContext<?> source)
            return ((ServerCommandSource) source.getSource()).hasPermissionLevel(2);
        if (sender instanceof ServerPlayerEntity player)
            return player.hasPermissionLevel(2);

        return false;
    }

    @Override
    public boolean hasPermission(Object sender, String permission) {
        if (sender instanceof CommandContext<?> source)
            return Permissions.check((ServerCommandSource) source.getSource(), permission);
        if (sender instanceof ServerPlayerEntity player)
            return Permissions.check(player, permission);

        return false;
    }

    @Override
    public void sendMessage(Object sender, String message) {
        if (sender instanceof ServerPlayerEntity player)
            player.sendMessage(Text.of(message));
        else if (sender instanceof CommandContext<?> context) {
            ServerCommandSource source = (ServerCommandSource) context.getSource();
            if (source.getPlayer() == null)
                source.sendMessage(Text.of(message.replaceAll(REPLACE_LEGACY_FORMATTING_CODES, "")));
            else
                source.sendMessage(Text.of(message));
        } else
            warn("Seems like we are trying to send a message to a sender which was not recognized (it is a " + sender.getClass().getSimpleName() + "). Please report this on GitHub issues!");
    }

    @Override
    public void sendMessage(Player player, String message) {
        ((PlayerEntity) player.getPlayer()).sendMessage(Text.of(message));
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
    public Loader getLoader() {
        return Loader.FABRIC;
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
    public void debug(String message) {
        debug(message, 1);
    }

    @Override
    public void debugVerbose(String message) {
        debug(message, 2);
    }

    @Override
    public void debugExtremelyVerbose(String message) {
        debug(message, 3);
    }

    private void debug(String message, int levelToLog) {
        if (debugLevel >= levelToLog) LOGGER.info("[DEBUG " + levelToLog + "] " + message);
    }
}
