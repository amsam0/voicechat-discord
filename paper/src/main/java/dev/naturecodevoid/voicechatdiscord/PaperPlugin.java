package dev.naturecodevoid.voicechatdiscord;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
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
import org.jetbrains.annotations.NotNull;

import static dev.naturecodevoid.voicechatdiscord.VoicechatDiscord.*;

public final class PaperPlugin extends JavaPlugin implements Listener, CommandExecutor {
    public static final Logger LOGGER = LogManager.getLogger(PLUGIN_ID);
    private Plugin voicechatPlugin;

    @SuppressWarnings({"DataFlowIssue"})
    @Override
    public void onEnable() {
        platform = new PaperPlatform();

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            voicechatPlugin = new Plugin();
            service.registerPlugin(voicechatPlugin);
            LOGGER.info("Successfully registered voicechat discord plugin");
        } else {
            LOGGER.error("Failed to register voicechat discord plugin");
        }

        getCommand("startdiscordvoicechat").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);

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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        runStartCommand(sender);

        return true;
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
