package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static dev.naturecodevoid.voicechatdiscord.Common.api;
import static dev.naturecodevoid.voicechatdiscord.Common.debugLevel;
import static dev.naturecodevoid.voicechatdiscord.FabricMod.LOGGER;

public class FabricPlatform extends Platform {
    @SuppressWarnings("rawtypes")
    @Override
    public boolean isValidPlayer(Object sender) {
        if (sender instanceof CommandContext source)
            return ((ServerCommandSource) source.getSource()).getPlayer() != null;
        return sender != null;
    }

    @SuppressWarnings("rawtypes")
    public ServerPlayer commandContextToPlayer(CommandContext context) {
        return api.fromServerPlayer(((ServerCommandSource) context.getSource()).getPlayer());
    }

    @Override
    public @Nullable Position getEntityPosition(ServerLevel level, UUID uuid) {
        ServerWorld world = (ServerWorld) level.getServerLevel();
        Entity entity = world.getEntity(uuid);
        return entity != null ?
                api.createPosition(
                        entity.getX(),
                        entity.getY(),
                        entity.getZ()
                )
                : null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean isOperator(Object sender) {
        if (sender instanceof CommandContext source)
            return ((ServerCommandSource) source.getSource()).hasPermissionLevel(2);
        if (sender instanceof ServerPlayerEntity player)
            return player.hasPermissionLevel(2);

        return false;
    }

    @Override
    public boolean hasPermission(Object sender, String permission) {
        // fabric doesn't have a permission system
        // maybe use LuckPerms API or something
        return false;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void sendMessage(Object sender, String message) {
        if (sender instanceof ServerPlayerEntity player)
            player.sendMessage(Text.of(message));
        else if (sender instanceof CommandContext context) {
            ServerCommandSource source = (ServerCommandSource) context.getSource();
            if (source.getPlayer() == null)
                source.sendMessage(Text.of(message.replaceAll("§([a-z]|[0-9]|[A-Z])", "")));
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
        if (debugLevel >= levelToLog) LOGGER.info("[DEBUG] " + message);
    }
}
