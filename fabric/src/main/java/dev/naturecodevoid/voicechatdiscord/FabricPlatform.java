package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static dev.naturecodevoid.voicechatdiscord.Common.api;
import static dev.naturecodevoid.voicechatdiscord.FabricMod.LOGGER;


public class FabricPlatform extends Platform {

    @Override
    public boolean isValidPlayer(Object sender) {
        return sender != null;
    }

    @Override
    public CompletableFuture<@Nullable EntityData> getEntityData(ServerLevel level, UUID uuid) {
        ServerWorld world  = (ServerWorld) level.getServerLevel();
        Entity      entity = world.getEntity(uuid);
        return CompletableFuture.completedFuture(entity != null ? new EntityData(uuid, api.createPosition(entity.getX(), entity.getY(), entity.getZ())) : null);
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
    public Object commandSourceToPlayerObject(Object source) {
        if (!(source instanceof ServerCommandSource))
            return null;
        return ((ServerCommandSource) source).getPlayer();
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
