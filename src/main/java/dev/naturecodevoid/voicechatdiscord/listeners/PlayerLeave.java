package dev.naturecodevoid.voicechatdiscord.listeners;

import dev.naturecodevoid.voicechatdiscord.Bot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import static dev.naturecodevoid.voicechatdiscord.BukkitPlugin.getBotForPlayer;

public class PlayerLeave implements Listener {
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        Bot bot = getBotForPlayer(e.getPlayer().getUniqueId());
        if (bot != null)
            bot.stop();
    }
}
