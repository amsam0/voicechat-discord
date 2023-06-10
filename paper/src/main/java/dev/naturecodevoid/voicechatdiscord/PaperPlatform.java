package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static dev.naturecodevoid.voicechatdiscord.Common.*;
import static dev.naturecodevoid.voicechatdiscord.PaperPlugin.LOGGER;

public class PaperPlatform extends Platform {
    @SuppressWarnings("rawtypes")
    @Override
    public boolean isValidPlayer(Object sender) {
        if (sender instanceof CommandContext context)
            return ((CommandSourceStack) context.getSource()).getPlayer() != null;
        return sender instanceof Player;
    }

    @SuppressWarnings("rawtypes")
    public ServerPlayer commandContextToPlayer(CommandContext context) {
        return api.fromServerPlayer(((CommandSourceStack) context.getSource()).getBukkitEntity());
    }

    public @Nullable Position getEntityPosition(ServerLevel level, UUID uuid) {
        if (level.getServerLevel() instanceof World world) {
            Entity entity = world.getEntity(uuid);
            return entity != null ?
                    api.createPosition(
                            entity.getLocation().getX(),
                            entity.getLocation().getY(),
                            entity.getLocation().getZ()
                    )
                    : null;
        }
        if (level.getServerLevel() instanceof net.minecraft.server.level.ServerLevel world) {
            net.minecraft.world.entity.Entity entity = world.getEntity(uuid);
            return entity != null ?
                    api.createPosition(
                            entity.getX(),
                            entity.getY(),
                            entity.getZ()
                    )
                    : null;
        }
        error("level is not World or ServerLevel, it is " + level.getClass().getSimpleName() + ". Please report this on GitHub Issues!");
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean isOperator(Object sender) {
        if (sender instanceof CommandContext context)
            return ((CommandSourceStack) context.getSource()).hasPermission(2);
        if (sender instanceof Permissible permissible)
            return permissible.isOp();

        return false;
    }

    @Override
    public boolean hasPermission(Object sender, String permission) {
        if (!(sender instanceof Permissible))
            return false;
        return ((Permissible) sender).hasPermission(permission);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void sendMessage(Object sender, String message) {
        if (sender instanceof CommandSender player)
            player.sendMessage(message);
        else if (sender instanceof CommandContext context) {
            CommandSourceStack source = (CommandSourceStack) context.getSource();
            if (source.getPlayer() == null)
                source.sendSystemMessage(Component.literal(message.replaceAll(REPLACE_LEGACY_FORMATTING_CODES, "")));
            else
                source.sendSystemMessage(Component.literal(message));
        } else
            warn("Seems like we are trying to send a message to a sender which was not recognized (it is a " + sender.getClass().getSimpleName() + "). Please report this on GitHub issues!");

    }

    @Override
    public void sendMessage(de.maxhenkel.voicechat.api.Player player, String message) {
        ((Player) player.getPlayer()).sendMessage(message);
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
    public String getLoader() {
        return "paper";
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
        if (debugLevel >= levelToLog) LOGGER.info("[DEBUG " + levelToLog + "] " + message);
    }
}
