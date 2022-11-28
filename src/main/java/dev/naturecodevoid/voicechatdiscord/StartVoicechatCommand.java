package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static dev.naturecodevoid.voicechatdiscord.BukkitPlugin.*;

public class StartVoicechatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("§cYou must be a player to use this command!");
            return true;
        }

        Player player = api.fromServerPlayer(sender);

        if (getBotForPlayer(player.getUuid()) != null) {
            sender.sendMessage("§cYou have already started a voice chat!");
            return true;
        }

        Bot bot = getAvailableBot();

        if (bot == null) {
            sender.sendMessage(
                    "§cThere are currently no bots available. You might want to contact your server owner to add more.");
            return true;
        }

        sender.sendMessage("§eStarting a voice chat...");

        new Thread(() -> {
            bot.login();
            bot.start(player, sender);
        }).start();

        return true;
    }
}
