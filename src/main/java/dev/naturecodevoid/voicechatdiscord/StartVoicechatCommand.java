package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

import static dev.naturecodevoid.voicechatdiscord.BukkitPlugin.*;

public class StartVoicechatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("§cYou must be a player to use this command!");
            return true;
        }

        Player player = api.fromServerPlayer(sender);

        if (connectedPlayers.containsKey(player.getUuid())) {
            sender.sendMessage("§cYou have already started a voice chat!");
            return true;
        }

        VoiceChannel channel = jda.getChannelById(VoiceChannel.class, vcId);
        Guild guild = channel.getGuild();
        AudioManager manager = guild.getAudioManager();

        EntityAudioChannel audioChannel = api.createEntityAudioChannel(UUID.randomUUID(), player);
        DiscordAudioHandler handler = new DiscordAudioHandler();

        manager.setSendingHandler(handler);
        manager.setReceivingHandler(handler);
        manager.openAudioConnection(channel);

        connectedPlayers.put(
                player.getUuid(),
                new ConnectedPlayerData(audioChannel, handler)
        );

        api.createAudioPlayer(
                audioChannel,
                api.createEncoder(),
                handler::provide20MsIncomingAudio
        ).startPlaying();

        LOGGER.info("Starting voice chat for " + sender.getName());
        sender.sendMessage("§aStarted a voice chat!");

        return true;
    }
}
