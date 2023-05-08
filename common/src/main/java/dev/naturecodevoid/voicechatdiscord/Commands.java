package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import org.jetbrains.annotations.Nullable;

import static dev.naturecodevoid.voicechatdiscord.Common.*;


// The start and stop commands for the Discord <-> SVC transfer system.
public class Commands {

    public static final String COMMAND = "dvc";


    public static boolean canExecuteDvc(Object source) {
        @Nullable Object       sourcePlayer = platform.commandSourceToPlayerObject(source);
        @Nullable ServerPlayer player       = sourcePlayer != null ? api.fromServerPlayer(sourcePlayer) : null;
        return player != null;
    }

    public static void executeDvc(Object source) throws CommandSyntaxException {
        @Nullable Object       sourcePlayer = platform.commandSourceToPlayerObject(source);
        @Nullable ServerPlayer player       = sourcePlayer != null ? api.fromServerPlayer(sourcePlayer) : null;
        if (player == null) {
            throw new SimpleCommandExceptionType(new LiteralMessage("You must be a player to use this command!")).create();
        }
        Bot bot = getBotForPlayer(player.getUuid());
        if (bot == null) {
            @Nullable VoicechatConnection connection = api.getConnectionOf(player);
            if (connection != null && connection.isInstalled()) {
                throw new SimpleCommandExceptionType(new LiteralMessage("You already have Simple Voice Chat installed." + ((! connection.isConnected()) || connection.isDisabled() ? " Consider enabling the connection." : ""))).create();
            } else {
                bot = getAvailableBot();
                if (bot == null) {
                    throw new SimpleCommandExceptionType(new LiteralMessage("There are currently no bots available. Consider contacting your server owner to add more.")).create();
                } else {
                    platform.sendMessage(sourcePlayer, "§eStarting a voice chat connection..." + (! bot.hasLoggedIn ? " This might take a few seconds to login to the bot." : "") + "§r");
                    bot.player = player;
                    final Bot finalBot = bot;
                    new Thread(() -> {
                        finalBot.login();
                        finalBot.start();
                    }).start();
                }
            }
        } else {
            bot.stop();
            platform.sendMessage(sourcePlayer, "§aSuccessfully stopped the bot!§r");
        }
    }


    public static boolean canExecuteDvcReload(Object source) {
        @Nullable Object       sourcePlayer = platform.commandSourceToPlayerObject(source);
        @Nullable ServerPlayer player       = sourcePlayer != null ? api.fromServerPlayer(sourcePlayer) : null;
        if (player == null) {return false;}
        return platform.isOperator(player.getPlayer()) || platform.hasPermission(player.getPlayer(), RELOAD_CONFIG_PERMISSION);
    }

    public static void executeDvcReload(Object source) throws CommandSyntaxException {
        @Nullable Object sourcePlayer = platform.commandSourceToPlayerObject(source);

        if (sourcePlayer != null && ! platform.isOperator(sourcePlayer) && ! platform.hasPermission(sourcePlayer, RELOAD_CONFIG_PERMISSION)) {
            throw new SimpleCommandExceptionType(new LiteralMessage("You do not have permission to use this command!")).create();
        }

        platform.sendMessage(sourcePlayer, "§eStopping bots...");

        new Thread(() -> {
            for (Bot bot : bots)
                if (bot.player != null)
                    platform.sendMessage(
                            bot.player,
                            "§cThe config is being reloaded which stops all bots. Please use §r§f/dvc §r§cto restart your session."
                    );
            stopBots();

            platform.sendMessage(sourcePlayer, "§aSuccessfully stopped bots! §eReloading config...");

            loadConfig();

            platform.sendMessage(sourcePlayer, "§aSuccessfully reloaded config!");
        }).start();
    }

}
