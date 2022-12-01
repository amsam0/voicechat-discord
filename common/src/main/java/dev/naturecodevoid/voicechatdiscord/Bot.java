package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.HashMap;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dev.naturecodevoid.voicechatdiscord.VoicechatDiscord.api;
import static dev.naturecodevoid.voicechatdiscord.VoicechatDiscord.platform;

public class Bot {
    private final String token;
    private final long vcId;
    protected Player player;
    protected OpusDecoder discordDecoder;
    protected OpusEncoder discordEncoder;
    protected HashMap<UUID, Queue<short[]>> outgoingAudio = new HashMap<>();
    protected Queue<short[]> incomingAudio = new ConcurrentLinkedQueue<>();
    protected JDA jda;
    private boolean hasLoggedIn = false;
    private AudioPlayer audioPlayer;
    private AudioManager manager;
    private HashMap<UUID, OpusDecoder> playerDecoders = new HashMap<>();

    public Bot(String token, long vcId) {
        this.token = token;
        this.vcId = vcId;
    }

    public OpusDecoder getPlayerDecoder(UUID playerUuid) {
        OpusDecoder decoder = playerDecoders.get(playerUuid);
        if (decoder == null) {
            decoder = api.createDecoder();
            playerDecoders.put(playerUuid, decoder);
        }
        return decoder;
    }

    public void login() {
        if (hasLoggedIn)
            return;

        try {
            jda = JDABuilder.createDefault(token).enableCache(CacheFlag.VOICE_STATE).build().awaitReady();
            hasLoggedIn = true;
        } catch (InterruptedException e) {
            platform.error("Failed to login to the bot using vc_id " + vcId);
            throw new RuntimeException(e);
        }
    }

    public void start(ServerPlayer player) {
        VoiceChannel channel = jda.getChannelById(VoiceChannel.class, vcId);
        if (channel == null) {
            platform.error(
                    "Please ensure that all voice channel IDs are valid, available to the bot and that they are actual voice channels.");
            platform.sendMessage(
                    player,
                    "§cThe provided voice channel ID seems to be invalid. Please make sure that it is available to the bot and that it is an actual voice channel."
            );
            return;
        }

        Guild guild = channel.getGuild();
        manager = guild.getAudioManager();

        DiscordAudioHandler handler = new DiscordAudioHandler(this);

        manager.setSendingHandler(handler);
        manager.setReceivingHandler(handler);
        manager.openAudioConnection(channel);

        discordEncoder = api.createEncoder();
        discordDecoder = api.createDecoder();

        EntityAudioChannel audioChannel = api.createEntityAudioChannel(player.getUuid(), player);
        audioPlayer = api.createAudioPlayer(
                audioChannel,
                api.createEncoder(),
                handler::provide20MsIncomingAudio
        );
        audioPlayer.startPlaying();

        this.player = player;

        platform.info("Starting voice chat for " + platform.getName(player));
        platform.sendMessage(
                player,
                "§aStarted a voice chat! Please join the following voice channel in discord:§r§f " + channel.getName()
        );
    }

    public void stop() {
        if (manager != null) {
            manager.setSendingHandler(null);
            manager.setReceivingHandler(null);
            manager.closeAudioConnection();
            manager = null;
        }

        if (audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.stopPlaying();
            audioPlayer = null;
        }

        player = null;

        if (discordDecoder != null) {
            discordDecoder.close();
            discordDecoder = null;
        }

        if (discordEncoder != null) {
            discordEncoder.close();
            discordEncoder = null;
        }

        for (OpusDecoder decoder : playerDecoders.values())
            decoder.close();

        playerDecoders = new HashMap<>();
        outgoingAudio = new HashMap<>();
        incomingAudio = new ConcurrentLinkedQueue<>();
    }
}
