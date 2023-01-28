package dev.naturecodevoid.voicechatdiscord;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import net.minecraft.commands.CommandSourceStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static dev.naturecodevoid.voicechatdiscord.Common.*;

public final class PaperPlugin extends JavaPlugin implements Listener {
    public static final Logger LOGGER = LogManager.getLogger(PLUGIN_ID);
    private static PaperPlugin INSTANCE;
    private VoicechatPlugin voicechatPlugin;

    public static PaperPlugin get() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        platform = new PaperPlatform();

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            voicechatPlugin = new VoicechatPlugin();
            service.registerPlugin(voicechatPlugin);
            LOGGER.info("Successfully registered voicechat discord plugin");
        } else {
            LOGGER.error("Failed to register voicechat discord plugin");
        }

        enable();

        Bukkit.getPluginManager().registerEvents(this, this);

        for (Commands.Command command : commands) {
            final PluginBrigadierCommand pluginBrigadierCommand = new PluginBrigadierCommand(
                    command.name(),
                    command.builder()
            );
            getServer().getCommandMap().register(getName(), pluginBrigadierCommand);
        }
        ((CraftServer) getServer()).syncCommands();
    }

    @Override
    public void onDisable() {
        disable();

        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);
            LOGGER.info("Successfully unregistered voicechat discord plugin");
        }
    }

    @SuppressWarnings({"UnstableApiUsage", "unchecked", "rawtypes"})
    @EventHandler
    public void onCommandRegistered(final CommandRegisteredEvent<BukkitBrigadierCommandSource> event) {
        if (!(event.getCommand() instanceof PluginBrigadierCommand pluginBrigadierCommand))
            return;

        final LiteralArgumentBuilder<CommandSourceStack> node = LiteralArgumentBuilder.literal(event.getCommandLabel());
        pluginBrigadierCommand.builder().accept(node);
        event.setLiteral((LiteralCommandNode) node.build());
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent e) {
        onPlayerLeave(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void playerRespawn(PlayerPostRespawnEvent e) {
        afterPlayerRespawn(api.fromServerPlayer(e.getPlayer()));
    }
}
