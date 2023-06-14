package dev.naturecodevoid.voicechatdiscord;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static dev.naturecodevoid.voicechatdiscord.Constants.REPLACE_LEGACY_FORMATTING_CODES;
import static dev.naturecodevoid.voicechatdiscord.Core.api;
import static dev.naturecodevoid.voicechatdiscord.PaperPlugin.LOGGER;

public class PaperPlatform implements Platform {
    public boolean isValidPlayer(Object sender) {
        if (sender instanceof CommandContext<?> context)
            return ((BukkitBrigadierCommandSource) context.getSource()).getBukkitEntity() instanceof Player;
        return sender instanceof Player;
    }

    public ServerPlayer commandContextToPlayer(CommandContext<?> context) {
        return api.fromServerPlayer(((BukkitBrigadierCommandSource) context.getSource()).getBukkitEntity());
    }

    public CompletableFuture<@Nullable Position> getEntityPosition(ServerLevel level, UUID uuid) {
        if (level.getServerLevel() instanceof World world) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    debugExtremelyVerbose("getting position for " + uuid);
                    return Bukkit.getScheduler().callSyncMethod(PaperPlugin.INSTANCE, () -> {
                        Entity entity = world.getEntity(uuid);
                        debugExtremelyVerbose("got position for " + uuid);
                        return entity != null ?
                                api.createPosition(
                                        entity.getLocation().getX(),
                                        entity.getLocation().getY(),
                                        entity.getLocation().getZ()
                                )
                                : null;
                    }).get();
                } catch (InterruptedException | ExecutionException e) {
                    return null;
                }
            });
        }
        if (level.getServerLevel() instanceof net.minecraft.server.level.ServerLevel world) {
            net.minecraft.world.entity.Entity entity = world.getEntity(uuid);
            return CompletableFuture.completedFuture(entity != null ?
                    api.createPosition(
                            entity.getX(),
                            entity.getY(),
                            entity.getZ()
                    )
                    : null);
        }
        error("level is not World or ServerLevel, it is " + level.getClass().getSimpleName() + ". Please report this on GitHub Issues!");
        return null;
    }

    public boolean isOperator(Object sender) {
        if (sender instanceof CommandContext<?> context)
            return ((BukkitBrigadierCommandSource) context.getSource()).getBukkitSender().isOp();
        if (sender instanceof Permissible permissible)
            return permissible.isOp();

        return false;
    }

    public boolean hasPermission(Object sender, String permission) {
        if (!(sender instanceof Permissible))
            return false;
        return ((Permissible) sender).hasPermission(permission);
    }

    public void sendMessage(Object sender, String message) {
        if (sender instanceof CommandSender player)
            player.sendMessage(message);
        else if (sender instanceof CommandContext<?> context) {
            BukkitBrigadierCommandSource source = (BukkitBrigadierCommandSource) context.getSource();
            if (source.getBukkitEntity() instanceof Player player)
                player.sendMessage(message.replaceAll(REPLACE_LEGACY_FORMATTING_CODES, ""));
            else
                source.getBukkitSender().sendMessage(message);
        } else
            warn("Seems like we are trying to send a message to a sender which was not recognized (it is a " + sender.getClass().getSimpleName() + "). Please report this on GitHub issues!");

    }

    public void sendMessage(de.maxhenkel.voicechat.api.Player player, String message) {
        ((Player) player.getPlayer()).sendMessage(message);
    }

    public String getName(de.maxhenkel.voicechat.api.Player player) {
        return ((Player) player.getPlayer()).getName();
    }

    public String getConfigPath() {
        return "plugins/voicechat-discord/config.yml";
    }

    public Loader getLoader() {
        return Loader.PAPER;
    }

    public void info(String message) {
        LOGGER.info(message);
    }

    public void warn(String message) {
        LOGGER.warn(message);
    }

    public void error(String message) {
        LOGGER.error(message);
    }
}
