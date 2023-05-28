package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static dev.naturecodevoid.voicechatdiscord.Common.api;
import static dev.naturecodevoid.voicechatdiscord.PaperPlugin.LOGGER;


public class PaperPlatform extends Platform {

    @Override
    public boolean isValidPlayer(Object sender) {
        return sender instanceof Player;
    }

    public CompletableFuture<@Nullable EntityData> getEntityData(ServerLevel level, UUID uuid) {
        // Depending on the Bukkit version, this will be different.
        if (level.getServerLevel() instanceof net.minecraft.server.level.ServerLevel world) {
            Entity entity = world.getEntity(uuid);
            return CompletableFuture.completedFuture(entity != null ? new EntityData(
                    uuid,
                    api.createPosition(entity.getX(),
                                       entity.getY(),
                                       entity.getZ()
                    )
            ) : null);
        } else if (level.getServerLevel() instanceof CraftWorld world) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return Bukkit.getScheduler().callSyncMethod(PaperPlugin.INSTANCE, () -> {
                        org.bukkit.entity.Entity entity = world.getEntity(uuid);
                        if (entity != null) {
                            Location location = entity.getLocation();
                            return new EntityData(
                                    uuid,
                                    api.createPosition(location.getX(), location.getY(), location.getZ())
                            );
                        } else {
                            return null;
                        }
                    }).get();
                } catch (InterruptedException | ExecutionException e) {
                    return null;
                }
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isOperator(Object sender) {
        if (!(sender instanceof Permissible))
            return false;
        return ((Permissible) sender).isOp();
    }

    @Override
    public boolean hasPermission(Object sender, String permission) {
        if (!(sender instanceof Permissible))
            return false;
        return ((Permissible) sender).hasPermission(permission);
    }

    @Override
    public void sendMessage(Object sender, String message) {
        if (!(sender instanceof Player)) {
            warn("Seems like we are trying to send a message to a sender which is not a Player. Please report this on GitHub issues!");
            return;
        }

        ((Player) sender).sendMessage(message);
    }

    @Override
    public void sendMessage(de.maxhenkel.voicechat.api.Player player, String message) {
        ((Player) player.getPlayer()).sendMessage(message);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public Object commandSourceToPlayerObject(Object source) {
        if (!(source instanceof CommandSourceStack))
            return null;
        return ((CommandSourceStack) source).getPlayer().getBukkitEntity();
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
