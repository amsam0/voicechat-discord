package dev.naturecodevoid.voicechatdiscord.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import static dev.naturecodevoid.voicechatdiscord.BukkitPlugin.connectedPlayers;

public class PlayerLeave implements Listener {
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        connectedPlayers.remove(e.getPlayer().getUniqueId());
    }
}
