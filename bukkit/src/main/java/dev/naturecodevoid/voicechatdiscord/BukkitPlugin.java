package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static dev.naturecodevoid.voicechatdiscord.VoicechatDiscord.*;

public final class BukkitPlugin extends JavaPlugin {
    public static final Logger LOGGER = LogManager.getLogger(PLUGIN_ID);
    private Plugin voicechatPlugin;

    @SuppressWarnings({"DataFlowIssue"})
    @Override
    public void onEnable() {
        platform = new BukkitPlatform();

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            voicechatPlugin = new Plugin();
            service.registerPlugin(voicechatPlugin);
            LOGGER.info("Successfully registered voicechat discord plugin");
        } else {
            LOGGER.error("Failed to register voicechat discord plugin");
        }

        getCommand("startdiscordvoicechat").setExecutor(new StartVoicechatCommand());
        Bukkit.getPluginManager().registerEvents(new PlayerLeave(), this);

        enable();
    }

    @Override
    public void onDisable() {
        disable();

        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);
            LOGGER.info("Successfully unregistered voicechat discord plugin");
        }
    }

    public static class StartVoicechatCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            runStartCommand(sender);

            return true;
        }
    }

    public static class PlayerLeave implements Listener {
        @EventHandler
        public void playerLeave(PlayerQuitEvent e) {
            onPlayerLeave(e.getPlayer().getUniqueId());
        }
    }
}
