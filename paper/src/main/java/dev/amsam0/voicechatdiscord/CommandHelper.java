package dev.amsam0.voicechatdiscord;

import com.mojang.brigadier.context.CommandContext;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public interface CommandHelper {
    void registerCommands();

    Entity bukkitEntity(CommandContext<?> context);

    CommandSender bukkitSender(CommandContext<?> context);
}
