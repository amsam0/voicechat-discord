package dev.amsam0.voicechatdiscord.post_1_20_6;

import com.mojang.brigadier.context.CommandContext;
import dev.amsam0.voicechatdiscord.CommandHelper;
import dev.amsam0.voicechatdiscord.PaperPlugin;
import dev.amsam0.voicechatdiscord.SubCommands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

@SuppressWarnings("UnstableApiUsage")
public class Post_1_20_6_CommandHelper implements CommandHelper {
    @Override
    public void registerCommands() {
        var manager = PaperPlugin.get().getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(SubCommands.build(Commands.literal("dvc")).build());
        });
    }

    @Override
    public Entity bukkitEntity(CommandContext<?> context) {
        return ((CommandSourceStack) context.getSource()).getExecutor();
    }

    @Override
    public CommandSender bukkitSender(CommandContext<?> context) {
        return ((CommandSourceStack) context.getSource()).getSender();
    }
}
