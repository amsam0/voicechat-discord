package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.audiolistener.AudioListener;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import dev.naturecodevoid.voicechatdiscord.audio.AudioBridge;
import dev.naturecodevoid.voicechatdiscord.audio.DiscordAudioHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import static dev.naturecodevoid.voicechatdiscord.Common.api;
import static dev.naturecodevoid.voicechatdiscord.Common.platform;


public class DiscordBot {

    // Helper for Discord bots to queue and poll audio streams.
    public final AudioBridge audioBridge = new AudioBridge();
    // The Discord bot token.
    private final String token;
    // The Discord guild voice channel id to play to and listen from.
    private final long vcId;
    // The player that this Discord bot it linked to.
    public ServerPlayer player;
    // The SVC opus audio decoder.
    public OpusDecoder discordDecoder;
    // The SVC opus audio encoder.
    public OpusEncoder discordEncoder;
    // The Discord bot.
    public JDA jda;
    // The SVC audio channel to play to.
    public EntityAudioChannel audioChannel;
    // The SVC audio player.
    public AudioPlayer audioPlayer;
    // Whether the Discord bot has logged in yet.
    public boolean hasLoggedIn = false;
    // The Discord voice manager.
    private AudioManager manager;
    // Handler for transferring data between Discord and SVC.
    private DiscordAudioHandler handler;
    // The SVC audio listener to listen from
    private AudioListener listener;

    public DiscordBot(String token, long vcId) {
        this.token = token;
        this.vcId = vcId;
    }


    // Logs into the Discord bot.
    public void login() {
        if (hasLoggedIn)
            return;

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
                    .enableCache(CacheFlag.VOICE_STATE)
                    .build().awaitReady();
            hasLoggedIn = true;
        } catch (Exception e) {
            platform.error("Failed to login to the bot using vc_id " + vcId, e);
            if (player != null) {
                platform.sendMessage(
                        player,
                        // The error message might contain the token, so let's be safe and only show it to console
                        "§cFailed to login to the bot. Please contact your server owner since they will be able to see the error message."
                );
                player = null;
            }
        }
    }

    // Starts the Discord <-> SVC audio transfer system.
    public void start() {
        if (!hasLoggedIn)
            return;

        VoiceChannel channel = jda.getChannelById(VoiceChannel.class, vcId);
        if (channel == null) {
            platform.error(
                    "Please ensure that all voice channel IDs are valid, available to the bot and that they are actual voice channels.");
            platform.sendMessage(
                    player,
                    "§cThe provided voice channel ID seems to be invalid or inaccessible to the bot. Please make sure that it is available to the bot and that it is an actual voice channel."
            );
            return;
        }

        Guild guild = channel.getGuild();
        manager = guild.getAudioManager();

        handler = new DiscordAudioHandler(this);

        manager.setSendingHandler(handler);
        manager.setReceivingHandler(handler);
        manager.openAudioConnection(channel);

        listener = api.playerAudioListenerBuilder()
                .setPacketListener(handler::handleOutgoingSoundPacket)
                .setPlayer(player.getUuid())
                .build();
        api.registerAudioListener(listener);

        discordEncoder = api.createEncoder();
        discordDecoder = api.createDecoder();

        audioChannel = api.createEntityAudioChannel(player.getUuid(), player);
        createAudioPlayer();

        platform.info("Started voice chat for " + platform.getName(player));
        platform.sendMessage(
                player,
                "§aStarted a voice chat! To stop it, use §r§f/dvc stop§r§a. Please join the following voice channel in discord: §r§f" + channel.getName()
        );
    }

    // Creates an SVC audio player and starts it.
    public void createAudioPlayer() {
        audioPlayer = api.createAudioPlayer(
                audioChannel,
                api.createEncoder(),
                handler::provide20MsIncomingAudio
        );
        audioPlayer.startPlaying();
    }

    // Stops the Discord <-> SVC audio transfer system and clears all queued audio.
    public void stop() {
        if (manager != null) {
            manager.setSendingHandler(null);
            manager.setReceivingHandler(null);
            manager.closeAudioConnection();
            manager = null;
        }

        if (audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.stopPlaying();
        }

        player = null;
        audioPlayer = null;
        audioChannel = null;
        handler = null;

        if (listener != null) {
            api.unregisterAudioListener(listener);
            listener = null;
        }

        if (discordDecoder != null) {
            discordDecoder.close();
            discordDecoder = null;
        }
        if (discordEncoder != null) {
            discordEncoder.close();
            discordEncoder = null;
        }

        audioBridge.clear();
    }

}
