package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;

public abstract class Platform {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean isValidPlayer(Object sender);

    public abstract boolean isValidPlayer(ServerPlayer player);

    public abstract boolean isOperator(Object sender);

    public abstract boolean hasPermission(Object sender, String permission);

    public abstract void sendMessage(Object sender, String message);

    public abstract void sendMessage(Player player, String message);

    public abstract ServerLevel getServerLevel(ServerPlayer player);

    public abstract Object commandSourceToPlayerObject(Object source);

    public abstract String getName(Player player);

    public abstract String getConfigPath();

    // Paper uses log4j, Fabric uses slf4j
    public abstract void info(String message);

    public abstract void warn(String message);

    public abstract void error(String message);

    public abstract void error(String message, Throwable throwable);
}
