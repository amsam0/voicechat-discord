package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.UUID;

import static dev.naturecodevoid.voicechatdiscord.Core.api;
import static dev.naturecodevoid.voicechatdiscord.FabricMod.LOGGER;

public class FabricPlatform implements Platform {
    public boolean isValidPlayer(Object sender) {
        if (sender instanceof CommandContext<?> source)
            return ((ServerCommandSource) source.getSource()).getPlayer() != null;
        return sender != null;
    }

    public ServerPlayer commandContextToPlayer(CommandContext<?> context) {
        return api.fromServerPlayer(((ServerCommandSource) context.getSource()).getPlayer());
    }

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

    public boolean isOperator(Object sender) {
        if (sender instanceof CommandContext<?> source)
            return ((ServerCommandSource) source.getSource()).hasPermissionLevel(2);
        if (sender instanceof ServerPlayerEntity player)
            return player.hasPermissionLevel(2);

        return false;
    }

    public boolean hasPermission(Object sender, String permission) {
        if (sender instanceof CommandContext<?> source)
            return Permissions.check((ServerCommandSource) source.getSource(), permission);
        if (sender instanceof ServerPlayerEntity player)
            return Permissions.check(player, permission);

        return false;
    }

    public void sendMessage(Object sender, String message) {
        if (sender instanceof ServerPlayerEntity player)
            player.sendMessage(toNative(mm(message)));
        else if (sender instanceof CommandContext<?> context) {
            ServerCommandSource source = (ServerCommandSource) context.getSource();
            source.sendMessage(toNative(mm(message)));
        } else
            warn("Seems like we are trying to send a message to a sender which was not recognized (it is a " + sender.getClass().getSimpleName() + "). Please report this on GitHub issues!");
    }

    public void sendMessage(Player player, String message) {
        ((PlayerEntity) player.getPlayer()).sendMessage(toNative(mm(message)));
    }

    public String getName(Player player) {
        return ((PlayerEntity) player.getPlayer()).getName().getString();
    }

    public String getConfigPath() {
        return "config/voicechat-discord.yml";
    }

    public Loader getLoader() {
        return Loader.FABRIC;
    }

    public void info(String message) {
        LOGGER.info(ansi(mm(message)));
    }

    public void infoRaw(String message) {
        LOGGER.info(message);
    }

    // warn and error will already be colored yellow and red respectfully

    public void warn(String message) {
        LOGGER.warn(message);
    }

    public void error(String message) {
        LOGGER.error(message);
    }

    private Text toNative(Component component) {
        var json = GsonComponentSerializer.gson().serialize(component); // serialize to string instead of JsonElement, the JsonElement won't be compatible with minecraft's JsonElement
        // for some reason loom doesn't remap fromJson calls to the appropriate intermediary name (maybe because we target 1.19? very weird, maybe some bug with subclasses)
        // to work around this, we manually find the intermediary method with reflection
        // if that fails, we try to call it normally
        // and if that fails, we try again to find it with reflection by looking for the correct method signature
        try {
            return (Text) Arrays.stream(Text.Serializer.class.getMethods())
                    .filter(method -> method.getName().equals("method_10877"))
                    .findFirst()
                    .orElseThrow()
                    .invoke(null, json);
        } catch (NoSuchElementException ignored) {
            debug("fromJson method not found with intermediary name, trying without reflection");
            try {
                return Text.Serializer.fromJson(json);
            } catch (NoSuchMethodError ignored2) {
                debug("fromJson method still not found! Resorting to looking for a method with the correct signature");
                try {
                    return (Text) Arrays.stream(Text.Serializer.class.getMethods())
                            .filter(method ->
                                    method.getParameterCount() == 1 &&
                                            method.getParameterTypes()[0].getName().contains("String") &&
                                            method.getReturnType().equals(MutableText.class)
                            )
                            .findFirst()
                            .orElseThrow()
                            .invoke(null, json);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
