package dev.amsam0.voicechatdiscord;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static dev.amsam0.voicechatdiscord.Constants.RELOAD_CONFIG_PERMISSION;
import static dev.amsam0.voicechatdiscord.Core.*;
import static dev.amsam0.voicechatdiscord.GroupManager.*;
import static dev.amsam0.voicechatdiscord.util.Util.getArgumentOr;

/**
 * Subcommands for /dvc
 */
public final class SubCommands {
    @SuppressWarnings("unchecked")
    public static <S> LiteralArgumentBuilder<S> build(LiteralArgumentBuilder<S> builder) {
        return (LiteralArgumentBuilder<S>) ((LiteralArgumentBuilder<Object>) builder)
                .then(literal("start").executes(wrapInTry(SubCommands::start)))
                .then(literal("stop").executes(wrapInTry(SubCommands::stop)))
                .then(literal("reloadconfig").executes(wrapInTry(SubCommands::reloadConfig)))
                .then(literal("checkforupdate").executes(wrapInTry(SubCommands::checkForUpdate)))
                .then(literal("togglewhisper").executes(wrapInTry(SubCommands::toggleWhisper)))
                .then(literal("group").executes(GroupCommands::help)
                        .then(literal("list").executes(wrapInTry(GroupCommands::list)))
                        .then(literal("create")
                                // Yeah, this is kind of a mess, all because we would have to use a mixin to add a custom ArgumentType
                                // so instead we just use literals for the group type
                                .then(argument("name", string()).executes(wrapInTry(GroupCommands.create(Group.Type.NORMAL)))
                                        .then(argument("password", string()).executes(wrapInTry(GroupCommands.create(Group.Type.NORMAL)))
                                                .then(literal("normal").executes(wrapInTry(GroupCommands.create(Group.Type.NORMAL)))
                                                        .then(argument("persistent", bool()).executes(wrapInTry(GroupCommands.create(Group.Type.NORMAL))))
                                                )
                                                .then(literal("open").executes(wrapInTry(GroupCommands.create(Group.Type.OPEN)))
                                                        .then(argument("persistent", bool()).executes(wrapInTry(GroupCommands.create(Group.Type.OPEN))))
                                                )
                                                .then(literal("isolated").executes(wrapInTry(GroupCommands.create(Group.Type.ISOLATED)))
                                                        .then(argument("persistent", bool()).executes(wrapInTry(GroupCommands.create(Group.Type.ISOLATED))))
                                                )
                                        )
                                )
                        )
                        .then(literal("join")
                                .then(argument("id", integer(1)).executes(wrapInTry(GroupCommands::join))
                                        .then(argument("password", string()).executes(wrapInTry(GroupCommands::join)))
                                )
                        )
                        .then(literal("info").executes(wrapInTry(GroupCommands::info)))
                        .then(literal("leave").executes(wrapInTry(GroupCommands::leave)))
                        .then(literal("remove")
                                .then(argument("id", integer(1)).executes(wrapInTry(GroupCommands::remove)))
                        )
                );
    }

    private static Command<Object> wrapInTry(Consumer<CommandContext<?>> function) {
        return (sender) -> {
            try {
                function.accept(sender);
            } catch (Throwable e) {
                platform.error(e.getMessage());
                platform.debug(e);
                platform.sendMessage(sender, "<red>An error occurred when running the command. Please check the console or tell your server owner to check the console.");
            }
            return 1;
        };
    }

    private static void start(CommandContext<?> sender) {
        if (!platform.isValidPlayer(sender)) {
            platform.sendMessage(sender, "<red>You must be a player to use this command!");
            return;
        }

        ServerPlayer player = platform.commandContextToPlayer(sender);

        DiscordBot bot = getBotForPlayer(player.getUuid(), true);

        DiscordBot botForPlayer = getBotForPlayer(player.getUuid());
        if (botForPlayer != null) {
            if (!botForPlayer.isStarted()) {
                platform.sendMessage(player, "<yellow>Your voice chat is currently starting.");
            } else {
                platform.sendMessage(player, "<red>You have already started a voice chat! <yellow>Restarting your session...");
                new Thread(() -> {
                    botForPlayer.stop();
                    try {
                        // Give some time for any songbird stuff to resolve
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                    botForPlayer.logInAndStart(player);
                }).start();
            }
            return;
        }

        if (bot == null) {
            platform.sendMessage(
                    player,
                    "<red>There are currently no bots available. You might want to contact your server owner to add more."
            );
            return;
        }

        platform.sendMessage(player, "<yellow>Starting a voice chat...");

        new Thread(() -> bot.logInAndStart(player)).start();
    }

    private static void stop(CommandContext<?> sender) {
        if (!platform.isValidPlayer(sender)) {
            platform.sendMessage(sender, "<red>You must be a player to use this command!");
            return;
        }

        ServerPlayer player = platform.commandContextToPlayer(sender);

        DiscordBot bot = getBotForPlayer(player.getUuid());
        if (bot == null || !bot.isStarted()) {
            platform.sendMessage(player, "<red>You must start a voice chat before you can use this command!");
            return;
        }

        platform.sendMessage(player, "<yellow>Stopping the bot...");

        new Thread(() -> {
            bot.stop();

            platform.sendMessage(sender, "<green>Successfully stopped the bot!");
        }).start();
    }

    private static void reloadConfig(CommandContext<?> sender) {
        if (!platform.isOperator(sender) && !platform.hasPermission(
                sender,
                RELOAD_CONFIG_PERMISSION
        )) {
            platform.sendMessage(
                    sender,
                    "<red>You must be an operator or have the `" + RELOAD_CONFIG_PERMISSION + "` permission to use this command!"
            );
            return;
        }

        platform.sendMessage(sender, "<yellow>Stopping bots...");

        new Thread(() -> {
            for (DiscordBot bot : bots)
                if (bot.player() != null)
                    platform.sendMessage(
                            bot.player(),
                            "<red>The config is being reloaded which stops all bots. Please use <white>/dvc start <red>to restart your session."
                    );

            clearBots();

            platform.sendMessage(sender, "<green>Successfully stopped bots! <yellow>Reloading config...");

            loadConfig();

            platform.sendMessage(
                    sender,
                    "<green>Successfully reloaded config! Using " + bots.size() + " bot" + (bots.size() != 1 ? "s" : "") + "."
            );
        }).start();
    }

    private static void checkForUpdate(CommandContext<?> sender) {
        if (!platform.isOperator(sender)) {
            platform.sendMessage(
                    sender,
                    "<red>You must be an operator to use this command!"
            );
            return;
        }

        platform.sendMessage(sender, "<yellow>Checking for update...");

        new Thread(() -> {
            if (UpdateChecker.checkForUpdate())
                platform.sendMessage(sender, Objects.requireNonNullElse(UpdateChecker.updateMessage, "<red>No update found."));
            else
                platform.sendMessage(sender, "<red>An error occurred when checking for updates. Check the console for the error message.");
        }).start();
    }

    private static void toggleWhisper(CommandContext<?> sender) {
        if (!platform.isValidPlayer(sender)) {
            platform.sendMessage(sender, "<red>You must be a player to use this command!");
            return;
        }

        ServerPlayer player = platform.commandContextToPlayer(sender);

        DiscordBot bot = getBotForPlayer(player.getUuid());
        if (bot == null || !bot.isStarted()) {
            platform.sendMessage(player, "<red>You must start a voice chat before you can use this command!");
            return;
        }

        var set = !bot.whispering();
        bot.whispering(set);

        platform.sendMessage(sender, set ? "<green>Started whispering!" : "<green>Stopped whispering!");
    }

    private static final class GroupCommands {
        private static boolean checkIfGroupsEnabled(CommandContext<?> sender) {
            if (!api.getServerConfig().getBoolean("enable_groups", true)) {
                platform.sendMessage(sender, "<red>Groups are currently disabled.");
                return true;
            }
            return false;
        }

        @SuppressWarnings("SameReturnValue")
        private static int help(CommandContext<?> sender) {
            platform.sendMessage(
                    sender,
                    """
                            <red>Available subcommands:
                             - `<white>/dvc group list<red>`: List groups
                             - `<white>/dvc group create <name> [password] [type] [persistent]<red>`: Create a group
                             - `<white>/dvc group join <ID><red>`: Join a group
                             - `<white>/dvc group info<red>`: Get info about your current group
                             - `<white>/dvc group leave<red>`: Leave your current group
                             - `<white>/dvc group remove <ID><red>`: Removes a persistent group if there is no one in it
                            See <white>https://github.com/amsam0/voicechat-discord#dvc-group<red> for more info on how to use these commands."""
            );
            return 1;
        }

        private static void list(CommandContext<?> sender) {
            if (checkIfGroupsEnabled(sender)) return;

            Collection<Group> apiGroups = api.getGroups();

            if (apiGroups.isEmpty())
                platform.sendMessage(sender, "<red>There are currently no groups.");
            else {
                StringBuilder groupsMessage = new StringBuilder("<green>Groups:\n");

                for (Group group : apiGroups) {
                    int friendlyId = groupFriendlyIds.get(group.getId());
                    platform.debugVerbose("Friendly ID for " + group.getId() + " (" + group.getName() + ") is " + friendlyId);

                    String playersMessage = "<red>No players";
                    List<ServerPlayer> players = groupPlayers.get(group.getId());
                    if (players == null)
                        playersMessage = "<red>Unable to get players";
                    else if (!players.isEmpty())
                        playersMessage = players.stream().map(player -> platform.getName(player)).collect(Collectors.joining(", "));

                    groupsMessage.append("<green> - ")
                            .append(group.getName())
                            .append(" (ID is ")
                            .append(friendlyId)
                            .append("): ")
                            .append(group.hasPassword() ? "<red>Has password" : "<green>No password")
                            .append(group.isPersistent() ? "<yellow>, persistent" : "")
                            .append(".<green> Group type is ")
                            .append(
                                    group.getType() == Group.Type.NORMAL ? "normal" :
                                            group.getType() == Group.Type.OPEN ? "open" :
                                                    group.getType() == Group.Type.ISOLATED ? "isolated" :
                                                            "unknown"
                            )
                            .append(". Players: ")
                            .append(playersMessage)
                            .append("\n");
                }

                platform.sendMessage(sender, groupsMessage.toString().trim());
            }
        }

        private static Consumer<CommandContext<?>> create(Group.Type type) {
            return (sender) -> {
                if (!platform.isValidPlayer(sender)) {
                    platform.sendMessage(sender, "<red>You must be a player to use this command!");
                    return;
                }

                if (checkIfGroupsEnabled(sender)) return;

                String name = sender.getArgument("name", String.class);
                String password = getArgumentOr(sender, "password", String.class, null);
                if (password != null)
                    if (password.trim().isEmpty())
                        password = null;
                Boolean persistent = getArgumentOr(sender, "persistent", Boolean.class, false);
                assert persistent != null;

                VoicechatConnection connection = Objects.requireNonNull(api.getConnectionOf(platform.commandContextToPlayer(sender)));
                if (connection.getGroup() != null) {
                    platform.sendMessage(sender, "<red>You are already in a group!");
                    return;
                }

                Group group = api.groupBuilder()
                        .setName(name)
                        .setPassword(password)
                        .setType(type)
                        .setPersistent(persistent)
                        .build();
                connection.setGroup(group);

                platform.sendMessage(sender, "<green>Successfully created the group!");
            };
        }

        private static void join(CommandContext<?> sender) {
            if (!platform.isValidPlayer(sender)) {
                platform.sendMessage(sender, "<red>You must be a player to use this command!");
                return;
            }

            if (checkIfGroupsEnabled(sender)) return;

            Integer friendlyId = sender.getArgument("id", Integer.class);
            UUID groupId = groupFriendlyIds.getKey(friendlyId);
            if (groupId == null) {
                platform.sendMessage(sender, "<red>Invalid group ID. Please use <white>/dvc group list<red> to see all groups.");
                return;
            }

            Group group = Objects.requireNonNull(api.getGroup(groupId));
            if (group.hasPassword()) {
                String inputPassword = getArgumentOr(sender, "password", String.class, null);
                if (inputPassword != null)
                    if (inputPassword.trim().isEmpty())
                        inputPassword = null;

                if (inputPassword == null) {
                    platform.sendMessage(sender, "<red>The group has a password, and you have not provided one. Please rerun the command, including the password.");
                    return;
                }

                String groupPassword = getPassword(group);
                if (groupPassword == null) {
                    platform.sendMessage(sender, "<red>Since the group has a password, we need to check if the password you supplied is correct. However, we failed to get the password for the group (the server owner can see the error in console). You may need to update Simple Voice Chat Discord Bridge.");
                    return;
                }

                if (!inputPassword.equals(groupPassword)) {
                    platform.sendMessage(sender, "<red>The password you provided is incorrect. You may want to surround the password in quotes if the password has spaces in it.");
                    return;
                }
            }

            VoicechatConnection connection = Objects.requireNonNull(api.getConnectionOf(platform.commandContextToPlayer(sender)));
            if (connection.getGroup() != null) {
                platform.sendMessage(sender, "<red>You are already in a group! Leave it using <white>/dvc group leave<red>, then join this group.");
                return;
            }
            var botForPlayer = getBotForPlayer(platform.commandContextToPlayer(sender).getUuid());
            if (!connection.isInstalled() && (botForPlayer == null || !botForPlayer.isStarted())) {
                platform.sendMessage(sender, "<red>You must have the mod installed or start a voice chat before you can use this command!");
                return;
            }
            connection.setGroup(group);

            platform.sendMessage(sender, "<green>Successfully joined group \"" + group.getName() + "\". Use <white>/dvc group info<green> to see info on the group, and <white>/dvc group leave<green> to leave the group.");
        }

        private static void info(CommandContext<?> sender) {
            if (!platform.isValidPlayer(sender)) {
                platform.sendMessage(sender, "<red>You must be a player to use this command!");
                return;
            }

            if (checkIfGroupsEnabled(sender)) return;

            VoicechatConnection connection = Objects.requireNonNull(api.getConnectionOf(platform.commandContextToPlayer(sender)));
            Group group = connection.getGroup();
            if (group == null) {
                platform.sendMessage(sender, "<red>You are not in a group!");
                return;
            }

            List<ServerPlayer> players = groupPlayers.get(group.getId());
            String playersMessage = players.stream().map(player -> platform.getName(player)).collect(Collectors.joining(", "));
            String message = "<green>You are currently in \"" + group.getName() + "\". It " +
                    (group.hasPassword() ? "<red>has a password<green>" : "does not have a password") + (group.isPersistent() ? " and is persistent." : ".") +
                    " Group type is " +
                    (group.getType() == Group.Type.NORMAL ? "normal" :
                            group.getType() == Group.Type.OPEN ? "open" :
                                    group.getType() == Group.Type.ISOLATED ? "isolated" :
                                            "unknown") +
                    ". Players: " + playersMessage;

            platform.sendMessage(sender, message);
        }

        private static void leave(CommandContext<?> sender) {
            if (!platform.isValidPlayer(sender)) {
                platform.sendMessage(sender, "<red>You must be a player to use this command!");
                return;
            }

            if (checkIfGroupsEnabled(sender)) return;

            VoicechatConnection connection = Objects.requireNonNull(api.getConnectionOf(platform.commandContextToPlayer(sender)));
            if (connection.getGroup() == null) {
                platform.sendMessage(sender, "<red>You are not in a group!");
                return;
            }
            connection.setGroup(null);

            platform.sendMessage(sender, "<green>Successfully left the group.");
        }

        private static void remove(CommandContext<?> sender) {
            if (checkIfGroupsEnabled(sender)) return;

            Integer friendlyId = sender.getArgument("id", Integer.class);
            UUID groupId = groupFriendlyIds.getKey(friendlyId);
            if (groupId == null) {
                platform.sendMessage(sender, "<red>Invalid group ID. Please use <white>/dvc group list<red> to see all groups.");
                return;
            }

            if (!api.removeGroup(groupId)) {
                platform.sendMessage(sender, "<red>Couldn't remove the group. This means it either has players in it or it is not persistent.");
                return;
            }

            platform.sendMessage(sender, "<green>Successfully removed the group!");
        }
    }
}
