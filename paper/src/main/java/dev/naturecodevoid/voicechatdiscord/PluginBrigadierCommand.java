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
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.command.VanillaCommandWrapper;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

import static dev.naturecodevoid.voicechatdiscord.PaperPlugin.get;

@SuppressWarnings("rawtypes")
@DefaultQualifier(NonNull.class)
final class PluginBrigadierCommand extends Command implements PluginIdentifiableCommand {
    private final Consumer<LiteralArgumentBuilder> builder;

    PluginBrigadierCommand(final String name, final Consumer<LiteralArgumentBuilder> builder) {
        super(name);
        this.builder = builder;
    }

    @Override
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        final String joined = String.join(" ", args);
        final String argsString = joined.isBlank() ? "" : " " + joined;
        ((CraftServer) Bukkit.getServer()).getServer().getCommands().performPrefixedCommand(
                VanillaCommandWrapper.getListener(sender),
                commandLabel + argsString,
                commandLabel
        );
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(final CommandSender sender, final String alias, final String[] args, final @Nullable Location location) {
        final String joined = String.join(" ", args);
        final String argsString = joined.isBlank() ? "" : joined;
        final CommandDispatcher<CommandSourceStack> dispatcher = ((CraftServer) Bukkit.getServer()).getServer().getCommands().getDispatcher();
        final ParseResults<CommandSourceStack> results = dispatcher.parse(
                new StringReader(alias + " " + argsString),
                VanillaCommandWrapper.getListener(sender)
        );
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
