package dev.naturecodevoid.voicechatdiscord.pre_1_20_6;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.naturecodevoid.voicechatdiscord.CommandHelper;
import dev.naturecodevoid.voicechatdiscord.PaperPlugin;
import dev.naturecodevoid.voicechatdiscord.SubCommands;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;

import static dev.naturecodevoid.voicechatdiscord.BukkitHelper.getCraftServer;
import static dev.naturecodevoid.voicechatdiscord.Core.platform;

@SuppressWarnings("removal") // new brigadier APIs didn't exist in 1.19.4
public class Pre_1_20_6_CommandHelper implements CommandHelper, Listener {
    @Override
    public void registerCommands() {
        var plugin = PaperPlugin.get();

        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getServer().getCommandMap().register(plugin.getName(), new DvcBrigadierCommand());
        try {
            getCraftServer().getMethod("syncCommands").invoke(plugin.getServer());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Entity bukkitEntity(CommandContext<?> context) {
        return ((BukkitBrigadierCommandSource) context.getSource()).getBukkitEntity();
    }

    @Override
    public CommandSender bukkitSender(CommandContext<?> context) {
        return ((BukkitBrigadierCommandSource) context.getSource()).getBukkitSender();
    }

    @SuppressWarnings({"unchecked", "rawtypes", "UnstableApiUsage"})
    @EventHandler
    public void onCommandRegistered(final CommandRegisteredEvent<BukkitBrigadierCommandSource> event) {
        if (!(event.getCommand() instanceof DvcBrigadierCommand))
            return;

        platform.debug("registering pluginBrigadierCommand: " + event.getCommandLabel());
        final LiteralArgumentBuilder<CommandSourceStack> node = SubCommands.build(LiteralArgumentBuilder.literal(event.getCommandLabel()));
        event.setLiteral((LiteralCommandNode) node.build());
    }
}
