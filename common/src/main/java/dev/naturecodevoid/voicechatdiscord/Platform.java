package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class Platform {

    public record EntityData(UUID uuid, Position position, boolean isPlayer) {}

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean isValidPlayer(Object sender);

    public abstract boolean isValidPlayer(ServerPlayer player);

    public abstract @Nullable EntityData getEntityData(ServerLevel level, UUID uuid);

    public abstract boolean isOperator(Object sender);

    public abstract boolean hasPermission(Object sender, String permission);

    public abstract void sendMessage(Object sender, String message);

    public abstract void sendMessage(Player player, String message);

    public abstract ServerLevel getServerLevel(ServerPlayer player);

    public abstract Object commandSourceToPlayerObject(Object source);

    public abstract String getName(Player player);

    public abstract String getConfigPath();

    // Paper uses log4j, fabric uses slf4j
    public abstract void info(String message);

    public abstract void warn(String message);

    public abstract void error(String message);

    public abstract void error(String message, Throwable throwable);
}
