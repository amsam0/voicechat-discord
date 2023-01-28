package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.maxhenkel.voicechat.api.ServerPlayer;

import java.util.function.Consumer;

import static dev.naturecodevoid.voicechatdiscord.Common.*;

public class Commands {
    @SuppressWarnings("rawtypes")
    private static void register(String name, Consumer<LiteralArgumentBuilder> builder) {
        commands.add(new Command(name, builder));
    }

    @SuppressWarnings("unchecked")
    protected static void registerCommands() {
        register("startdiscordvoicechat", literal -> literal.executes(context -> {
            Object sender = platform.commandSourceToPlayerObject(context.getSource());

            if (!platform.isValidPlayer(sender)) {
                platform.sendMessage(sender, "§cYou must be a player to use this command!");
                return 1;
            }

            ServerPlayer player = api.fromServerPlayer(sender);

            Bot botForPlayer = getBotForPlayer(player.getUuid());
            if (botForPlayer != null) {
                platform.sendMessage(player, "§cYou have already started a voice chat! §eRestarting your session...");
                botForPlayer.stop();
            }

            Bot bot = getAvailableBot();

            if (bot == null) {
                platform.sendMessage(
                        player,
                        "§cThere are currently no bots available. You might want to contact your server owner to add more."
                );
                return 1;
            }

            if (botForPlayer == null)
                platform.sendMessage(
                        player,
                        "§eStarting a voice chat..." + (!bot.hasLoggedIn ? " this might take a moment since we have to login to the bot." : "")
                );

            bot.player = player;
            new Thread(() -> {
                bot.login();
                bot.start();
            }).start();

            return 1;
        }));

        register("stopdiscordvoicechat", literal -> literal.executes(context -> {
            Object sender = platform.commandSourceToPlayerObject(context.getSource());

            if (!platform.isValidPlayer(sender)) {
                platform.sendMessage(sender, "§cYou must be a player to use this command!");
                return 1;
            }

            ServerPlayer player = api.fromServerPlayer(sender);

            Bot bot = getBotForPlayer(player.getUuid());
            if (bot == null) {
                platform.sendMessage(player, "§cYou must start a voice chat before you can use this command!");
                return 1;
            }

            platform.sendMessage(player, "§eStopping the bot...");

            new Thread(() -> {
                bot.stop();

                platform.sendMessage(sender, "§aSuccessfully stopped the bot!");
            }).start();

            return 1;
        }));

        register("reloaddiscordvoicechatconfig", literal -> literal.executes(context -> {
            Object sender = platform.commandSourceToPlayerObject(context.getSource());

            if (!platform.isOperator(sender) && !platform.hasPermission(
                    sender,
                    RELOAD_CONFIG_PERMISSION
            )) {
                platform.sendMessage(
                        sender,
                        "§cYou must be an operator or have the `" + RELOAD_CONFIG_PERMISSION + "` permission to use this command!"
                );
                return 1;
            }

            platform.sendMessage(sender, "§eStopping bots...");

            new Thread(() -> {
                for (Bot bot : bots)
                    if (bot.player != null)
                        platform.sendMessage(
                                bot.player,
                                "§cThe config is being reloaded which stops all bots. Please use §r§f/startdiscordvoicechat §r§cto restart your session."
                        );
                stopBots();

                platform.sendMessage(sender, "§aSuccessfully stopped bots! §eReloading config...");

                loadConfig();

                platform.sendMessage(sender, "§aSuccessfully reloaded config!");
            }).start();

            return 1;
        }));
    }

    @SuppressWarnings("rawtypes")
    public record Command(String name, Consumer<LiteralArgumentBuilder> builder) {
        public String name() {
            return name;
        }

        public Consumer<LiteralArgumentBuilder> builder() {
            return builder;
        }
    }
}
