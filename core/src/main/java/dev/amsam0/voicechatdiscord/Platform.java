package dev.amsam0.voicechatdiscord;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static dev.amsam0.voicechatdiscord.Core.debugLevel;

public interface Platform {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isValidPlayer(Object sender);

    ServerPlayer commandContextToPlayer(CommandContext<?> context);

    @Nullable
    Position getEntityPosition(ServerLevel level, UUID uuid);

    boolean isOperator(Object sender);

    boolean hasPermission(Object sender, String permission);

    void sendMessage(Object sender, String message);

    void sendMessage(Player player, String message);

    String getName(Player player);

    String getConfigPath();

    Loader getLoader();

    // Paper uses log4j, Fabric uses slf4j
    void info(String message);

    void infoRaw(String message);

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

    @SuppressWarnings("CallToPrintStackTrace")
    default void debug(Throwable throwable) {
        if (debugLevel >= 1)
            throwable.printStackTrace();
    }

    private void debug(String message, int levelToLog) {
        // debugs are used so frequently and without color that there's no point in using minimessage
        if (debugLevel >= levelToLog) infoRaw("[DEBUG " + levelToLog + "] " + message);
    }

    default Component mm(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    default String ansi(Component component) {
        return ANSIComponentSerializer.ansi().serialize(component);
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
