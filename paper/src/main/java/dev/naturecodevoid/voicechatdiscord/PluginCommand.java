package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;


@DefaultQualifier(NonNull.class)
class PluginCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try {
            if (args.length == 0) {
                Commands.executeDvc(sender);
            } else if (args.length == 1 && args[0].equals("reload")) {
                Commands.executeDvcReload(sender);
            } else {
                throw new SimpleCommandExceptionType(new LiteralMessage("Unexpected arguments.")).create();
            }
        } catch (CommandSyntaxException e) {
            sender.sendMessage("§c" + e.getMessage() + "§r");
            return false;
        }
        return true;
    }

}
