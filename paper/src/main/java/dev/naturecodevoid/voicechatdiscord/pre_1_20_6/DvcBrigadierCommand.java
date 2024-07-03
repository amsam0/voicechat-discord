package dev.naturecodevoid.voicechatdiscord.pre_1_20_6;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static dev.naturecodevoid.voicechatdiscord.BukkitHelper.getCraftServer;
import static dev.naturecodevoid.voicechatdiscord.BukkitHelper.getVanillaCommandWrapper;
import static dev.naturecodevoid.voicechatdiscord.Core.platform;
import static dev.naturecodevoid.voicechatdiscord.PaperPlugin.get;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@DefaultQualifier(NonNull.class)
public final class DvcBrigadierCommand extends Command implements PluginIdentifiableCommand {
    public DvcBrigadierCommand() {
        super("dvc");
    }

    private static Object getListener(CommandSender sender) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> vanillaCommandWrapper = getVanillaCommandWrapper();
        return vanillaCommandWrapper.getMethod("getListener", CommandSender.class).invoke(null, sender);
    }

    private static Object getCommands() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Object minecraftServer = getCraftServer().getMethod("getServer").invoke(Bukkit.getServer());

        // Run MinecraftServer#getCommands
        return Arrays.stream(minecraftServer.getClass().getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .filter(method -> {
                    try {
                        return method.getReturnType() == Commands.class;
                    } catch (NoClassDefFoundError ignored) {
                        platform.debugVerbose("method returns " + method.getReturnType().getName());
                        return method.getReturnType().getName().equals("net.minecraft.commands.CommandDispatcher") ||
                                method.getReturnType().getName().equals("net.minecraft.commands.Commands");
                    }
                })
                .findFirst()
                .get()
                .invoke(minecraftServer);
    }

    @Override
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        platform.debug("executing dvc brigadier command");
        final String joined = String.join(" ", args);
        final String argsString = joined.isBlank() ? "" : " " + joined;

        try {
            Object commands = getCommands();

            // Run Commands#performPrefixedCommand
            Arrays.stream(commands.getClass().getMethods())
                    .filter(method -> method.getParameterCount() == 3)
                    .filter(method -> {
                        try {
                            return Arrays.equals(method.getParameterTypes(), new Class[]{CommandSourceStack.class, String.class, String.class});
                        } catch (NoClassDefFoundError ignored) {
                            var types = method.getParameterTypes();
                            platform.debugVerbose("method parameter types: " + Arrays.toString(types));
                            return (
                                    types[0].getName().equals("net.minecraft.commands.CommandListenerWrapper") ||
                                            types[0].getName().equals("net.minecraft.commands.CommandSourceStack")
                            ) && types[1] == String.class && types[2] == String.class;
                        }
                    })
                    .findFirst()
                    .get()
                    .invoke(commands, getListener(sender), commandLabel + argsString, commandLabel);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            platform.error("Unable to run brigadier command: " + e.getMessage());
            platform.debugStackTrace(e);
            platform.sendMessage(sender, "<red>Unable to run command. The addon needs to be updated. Please tell your server owner to create a GitHub issue with logs attached.");
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull List<String> tabComplete(final CommandSender sender, final String alias, final String[] args, final @Nullable Location location) {
        platform.debug("getting tab complete for dvc brigadier command");
        final String joined = String.join(" ", args);
        final String argsString = joined.isBlank() ? "" : joined;

        try {
            Object commands = getCommands();

            // Run Commands#getDispatcher
            // CommandDispatcher is in brigadier so it won't be obfuscated probably
            // this means we don't have to do weird reflection crap 😁
            CommandDispatcher<?> dispatcher = (CommandDispatcher<?>) Arrays.stream(commands.getClass().getMethods())
                    .filter(m -> m.getParameterCount() == 0)
                    .filter(method -> method.getReturnType() == CommandDispatcher.class)
                    .findFirst()
                    .get()
                    .invoke(commands);

            // Run CommandDispatcher#parse
            ParseResults<?> parseResults = (ParseResults<?>) dispatcher.getClass()
                    .getMethod("parse", StringReader.class, Object.class)
                    .invoke(dispatcher, new StringReader(alias + " " + argsString), getListener(sender));

            // Run CommandDispatcher#getCompletionSuggestions
            CompletableFuture<Suggestions> suggestions = (CompletableFuture<Suggestions>) dispatcher.getClass()
                    .getMethod("getCompletionSuggestions", ParseResults.class)
                    .invoke(dispatcher, parseResults);

            return suggestions
                    .thenApply(result -> result.getList().stream().map(Suggestion::getText).toList())
                    .join();
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            platform.error("Unable to get suggestions for brigadier command: " + e.getMessage());
            platform.debugStackTrace(e);
            platform.sendMessage(sender, "<red>Unable to get suggestions. The addon needs to be updated. Please tell your server owner to create a GitHub issue with logs attached.");
            return List.of();
        }
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return get();
    }
}
