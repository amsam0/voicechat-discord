package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static dev.naturecodevoid.voicechatdiscord.Core.debugLevel;

public interface Platform {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isValidPlayer(Object sender);

    ServerPlayer commandContextToPlayer(CommandContext<?> context);

    CompletableFuture<@Nullable Position> getEntityPosition(ServerLevel level, UUID uuid);

    boolean isOperator(Object sender);

    boolean hasPermission(Object sender, String permission);

    void sendMessage(Object sender, String message);

    void sendMessage(Player player, String message);

    String getName(Player player);

    String getConfigPath();

    Loader getLoader();

    // Paper uses log4j, Fabric uses slf4j
    void info(String message);

    void warn(String message);

    void error(String message);

    default void debug(String message) {
        debug(message, 1);
    }

    default void debugVerbose(String message) {
        debug(message, 2);
    }

    default void debugExtremelyVerbose(String message) {
        debug(message, 3);
    }

    private void debug(String message, int levelToLog) {
        if (debugLevel >= levelToLog) info("[DEBUG " + levelToLog + "] " + message);
    }

    enum Loader {
        PAPER("paper"),
        FABRIC("fabric");

        public final String name;

        Loader(String name) {
            this.name = name;
        }
    }
}
