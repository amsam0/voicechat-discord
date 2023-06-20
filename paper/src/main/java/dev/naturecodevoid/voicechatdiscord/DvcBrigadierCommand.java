package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
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

import static dev.naturecodevoid.voicechatdiscord.Core.platform;
import static dev.naturecodevoid.voicechatdiscord.PaperPlugin.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@DefaultQualifier(NonNull.class)
final class DvcBrigadierCommand extends Command implements PluginIdentifiableCommand {
    DvcBrigadierCommand() {
        super("dvc");
    }

    private static CommandSourceStack getListener(CommandSender sender) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> vanillaCommandWrapper = getVanillaCommandWrapper();
        return (CommandSourceStack) vanillaCommandWrapper.getMethod("getListener", CommandSender.class).invoke(null, sender);
    }

    @Override
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        platform.debug("executing dvc brigadier command");
        final String joined = String.join(" ", args);
        final String argsString = joined.isBlank() ? "" : " " + joined;

        try {
            MinecraftServer minecraftServer = (MinecraftServer) getCraftServer().getMethod("getServer").invoke(Bukkit.getServer());
            Commands commands = (Commands) Arrays.stream(MinecraftServer.class.getMethods())
                    .filter(method -> method.getReturnType() == Commands.class) // getCommands
                    .findFirst()
                    .get()
                    .invoke(minecraftServer);
            Arrays.stream(Commands.class.getMethods())
                    .filter(method -> Arrays.equals(method.getParameterTypes(), new Class[]{CommandSourceStack.class, String.class, String.class})) // performPrefixedCommand
                    .findFirst()
                    .get()
                    .invoke(commands, getListener(sender), commandLabel + argsString, commandLabel);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
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
            MinecraftServer minecraftServer = (MinecraftServer) getCraftServer().getMethod("getServer").invoke(Bukkit.getServer());
            Commands commands = (Commands) Arrays.stream(MinecraftServer.class.getMethods())
                    .filter(method -> method.getReturnType() == Commands.class) // getCommands
                    .findFirst()
                    .get()
                    .invoke(minecraftServer);
            CommandDispatcher<CommandSourceStack> dispatcher = (CommandDispatcher<CommandSourceStack>) Arrays.stream(Commands.class.getMethods())
                    .filter(method -> method.getReturnType() == CommandDispatcher.class) // getDispatcher
                    .findFirst()
                    .get()
                    .invoke(commands);

            return dispatcher.getCompletionSuggestions(dispatcher.parse(new StringReader(alias + " " + argsString), getListener(sender)))
                    .thenApply(result -> result.getList().stream().map(Suggestion::getText).toList())
                    .join();
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return get();
    }
}
