package dev.naturecodevoid.voicechatdiscord;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static dev.naturecodevoid.voicechatdiscord.Common.*;


public final class PaperPlugin extends JavaPlugin implements Listener {
    public static final Logger LOGGER = LogManager.getLogger(PLUGIN_ID);
    private VoicechatPlugin voicechatPlugin;


    @Override
    public void onEnable() {
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

        org.bukkit.command.PluginCommand command = Bukkit.getPluginCommand(Commands.COMMAND);
        assert command != null;
        command.setExecutor(new PluginCommand());

    }

    @Override
    public void onDisable() {
        disable();

        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);
            LOGGER.info("Successfully unregistered voicechat discord plugin");
        }
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
