package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class Platform {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean isValidPlayer(Object sender);

    public abstract ServerPlayer commandContextToPlayer(CommandContext<?> context);

    public abstract @Nullable Position getEntityPosition(ServerLevel level, UUID uuid);

    public abstract boolean isOperator(Object sender);

    public abstract boolean hasPermission(Object sender, String permission);

    public abstract void sendMessage(Object sender, String message);

    public abstract void sendMessage(Player player, String message);

    public abstract String getName(Player player);

    public abstract String getConfigPath();

    /**
     * Loader used for filtering versions on Modrinth
     */
    public abstract Loader getLoader();

    // Paper uses log4j, Fabric uses slf4j
    public abstract void info(String message);

    public abstract void warn(String message);

    public abstract void error(String message);

    public abstract void debug(String message);

    public abstract void debugVerbose(String message);

    public abstract void debugExtremelyVerbose(String message);

    public enum Loader {
        PAPER("paper"),
        FABRIC("fabric");

        public final String name;

        Loader(String name) {
            this.name = name;
        }
    }
}
