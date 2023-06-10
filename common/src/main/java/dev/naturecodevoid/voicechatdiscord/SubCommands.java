package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.maxhenkel.voicechat.api.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static dev.naturecodevoid.voicechatdiscord.Common.*;

/**
 * Subcommands for /dvc
 */
public class SubCommands {
    @SuppressWarnings({"unchecked", "CallToPrintStackTrace"})
    protected static List<SubCommand> getSubCommands() {
        List<SubCommand> subCommands = new ArrayList<>();

        // Each command is a subcommand on the /dvc command; this is handled by the platform implementation

        subCommands.add(new SubCommand("start", literal -> literal.executes(sender -> {
            try {
                if (!platform.isValidPlayer(sender)) {
                    platform.sendMessage(sender, "§cYou must be a player to use this command!");
                    return 1;
                }

                ServerPlayer player = platform.commandContextToPlayer(sender);

                DiscordBot bot = getBotForPlayer(player.getUuid(), true);

                DiscordBot botForPlayer = getBotForPlayer(player.getUuid());
                if (botForPlayer != null) {
                    platform.sendMessage(player, "§cYou have already started a voice chat! §eRestarting your session...");
                    botForPlayer.stop();
                }

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
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        })));

        subCommands.add(new SubCommand("stop", literal -> literal.executes(sender -> {
            try {
                if (!platform.isValidPlayer(sender)) {
                    platform.sendMessage(sender, "§cYou must be a player to use this command!");
                    return 1;
                }

                ServerPlayer player = platform.commandContextToPlayer(sender);

                DiscordBot bot = getBotForPlayer(player.getUuid());
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
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        })));

        subCommands.add(new SubCommand("reloadconfig", literal -> literal.executes(sender -> {
            try {
                if (!platform.isOperator(sender) && !platform.hasPermission(
                        sender,
                        Constants.RELOAD_CONFIG_PERMISSION
                )) {
                    platform.sendMessage(
                            sender,
                            "§cYou must be an operator or have the `" + Constants.RELOAD_CONFIG_PERMISSION + "` permission to use this command!"
                    );
                    return 1;
                }

                platform.sendMessage(sender, "§eStopping bots...");

                new Thread(() -> {
                    for (DiscordBot bot : bots)
                        if (bot.player != null)
                            platform.sendMessage(
                                    bot.player,
                                    "§cThe config is being reloaded which stops all bots. Please use §r§f/dvc start §r§cto restart your session."
                            );
                    stopBots();

                    platform.sendMessage(sender, "§aSuccessfully stopped bots! §eReloading config...");

                    loadConfig();

                    platform.sendMessage(
                            sender,
                            "§aSuccessfully reloaded config! Using " + bots.size() + " bot" + (bots.size() != 1 ? "s" : "") + "."
                    );
                }).start();

                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        })));

        subCommands.add(new SubCommand("checkforupdate", literal -> literal.executes(sender -> {
            try {
                if (!platform.isOperator(sender)) {
                    platform.sendMessage(
                            sender,
                            "§cYou must be an operator to use this command!"
                    );
                    return 1;
                }

                platform.sendMessage(sender, "§eChecking for update...");

                new Thread(() -> {
                    UpdateChecker.checkForUpdate();

                    platform.sendMessage(sender, Objects.requireNonNullElse(UpdateChecker.updateMessage, "§cNo update found. (or an error occurred, you might want to check the console)"));
                }).start();

                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        })));

        return subCommands;
    }

    @SuppressWarnings("rawtypes")
    public record SubCommand(String name, Function<LiteralArgumentBuilder, ArgumentBuilder> builder) {
        public String name() {
            return name;
        }

        public Function<LiteralArgumentBuilder, ArgumentBuilder> builder() {
            return builder;
        }
    }
}
