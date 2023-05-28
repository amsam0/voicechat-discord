package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public abstract class Platform {

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean isValidPlayer(Object sender);

    public abstract CompletableFuture<@Nullable EntityData> getEntityData(ServerLevel level, UUID uuid);

    public abstract boolean isOperator(Object sender);

    public abstract boolean hasPermission(Object sender, String permission);

    public abstract void sendMessage(Object sender, String message);

    public abstract void sendMessage(Player player, String message);

    public abstract Object commandSourceToPlayerObject(Object source);

    public abstract String getName(Player player);

    public abstract String getConfigPath();

    // Paper uses log4j, Fabric uses slf4j
    public abstract void info(String message);

    public abstract void warn(String message);

    public abstract void error(String message);

    public abstract void error(String message, Throwable throwable);

    public record EntityData(UUID uuid, Position position) {
    }

}
