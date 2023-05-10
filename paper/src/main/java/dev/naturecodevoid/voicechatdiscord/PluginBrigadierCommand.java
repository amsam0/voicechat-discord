package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.commands.CommandSourceStack;
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
import java.util.List;
import java.util.function.Consumer;

import static dev.naturecodevoid.voicechatdiscord.PaperPlugin.*;


@SuppressWarnings("rawtypes")
@DefaultQualifier(NonNull.class)
final class PluginBrigadierCommand extends Command implements PluginIdentifiableCommand {

    private final Consumer<LiteralArgumentBuilder> builder;

    PluginBrigadierCommand(final String name, final Consumer<LiteralArgumentBuilder> builder) {
        super(name);
        this.builder = builder;
    }

    @SuppressWarnings("JavaReflectionInvocation")
    private static CommandSourceStack getListener(CommandSender sender) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object dedicatedServer = getCraftServer().getMethod("getServer").invoke(Bukkit.getServer());
        Object commands = dedicatedServer.getClass().getMethod("getCommands").invoke(dedicatedServer);
        Class<?> vanillaCommandWrapper = getVanillaCommandWrapper();
        return (CommandSourceStack) vanillaCommandWrapper.getMethod("getListener").invoke(null, sender);
    }

    @SuppressWarnings("JavaReflectionInvocation")
    @Override
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        final String joined = String.join(" ", args);
        final String argsString = joined.isBlank() ? "" : " " + joined;

        try {
            Object dedicatedServer = getCraftServer().getMethod("getServer").invoke(Bukkit.getServer());
            Object commands = dedicatedServer.getClass().getMethod("getCommands").invoke(dedicatedServer);

            commands.getClass().getMethod("performPrefixedCommand").invoke(
                    commands,
                    getListener(sender),
                    commandLabel + argsString,
                    commandLabel
            );
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull List<String> tabComplete(final CommandSender sender, final String alias, final String[] args, final @Nullable Location location) {
        final String joined = String.join(" ", args);
        final String argsString = joined.isBlank() ? "" : joined;

        final CommandDispatcher<CommandSourceStack> dispatcher;
        try {
            Object dedicatedServer = getCraftServer().getMethod("getServer").invoke(Bukkit.getServer());
            Object commands = dedicatedServer.getClass().getMethod("getCommands").invoke(dedicatedServer);

            dispatcher = (CommandDispatcher<CommandSourceStack>) commands.getClass().getMethod("getDispatcher").invoke(
                    commands);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        final ParseResults<CommandSourceStack> results;
        try {
            results = dispatcher.parse(
                    new StringReader(alias + " " + argsString),
                    getListener(sender)
            );
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return dispatcher.getCompletionSuggestions(results)
                .thenApply(result -> result.getList().stream().map(Suggestion::getText).toList())
                .join();
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return get();
    }

    public Consumer<LiteralArgumentBuilder> builder() {
        return this.builder;
    }

}
