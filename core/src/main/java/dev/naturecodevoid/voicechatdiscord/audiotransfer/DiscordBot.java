package dev.naturecodevoid.voicechatdiscord.audiotransfer;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.audiolistener.AudioListener;
import de.maxhenkel.voicechat.api.audiosender.AudioSender;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.SoundPacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.OpusPacket;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dev.naturecodevoid.voicechatdiscord.Core.api;
import static dev.naturecodevoid.voicechatdiscord.Core.platform;

public final class DiscordBot implements AudioSendHandler, AudioReceiveHandler {
    /**
     * The Discord bot token.
     */
    private final String token;
    /**
     * The Discord guild voice channel id to play to and listen from.
     */
    private final long vcId;
    /**
     * Sources of audio going to Discord
     */
    private final HashMap<UUID, AudioSource> audioSources = new HashMap<>();
    /**
     * The player that this Discord bot is linked to.
     */
    public ServerPlayer player;
    /**
     * The opus audio encoder used for encoding audio going to Discord.
     */
    public OpusEncoder discordEncoder;
    /**
     * The JDA instance for this
     */
    public JDA jda;
    /**
     * Whether the Discord bot has logged in yet.
     */
    public boolean hasLoggedIn = false;
    /**
     * The SVC audio sender used to send audio to SVC.
     */
    public AudioSender sender;
    /**
     * The last time (unix timestamp) that audio was sent to the audio sender.
     */
    private long lastTimeAudioProvidedToSVC = 0;
    /**
     * The last time (unix timestamp) that audio was sent to Discord.
     */
    private long lastTimeAudioProvidedToDiscord = 0;
    /**
     * A thread that checks every 100ms if the audio sender, discord encoder and audio source decoders should be reset.
     */
    private Thread resetWatcher;
    /**
     * The Discord voice manager
     */
    private AudioManager manager;
    /**
     * The SVC audio listener to listen for outgoing (to Discord) audio.
     */
    private AudioListener listener;

    public DiscordBot(String token, long vcId) {
        this.token = token.trim();
        this.vcId = vcId;
    }

    /**
     * Logs into the Discord bot.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public void login() {
        if (hasLoggedIn)
            return;

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
                    .enableCache(CacheFlag.VOICE_STATE)
                    .build().awaitReady();
            hasLoggedIn = true;
            platform.debug("logged into the bot with vc_id " + vcId);
        } catch (Throwable e) {
            platform.error("Failed to login to the bot using vc_id " + vcId);
            e.printStackTrace();
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

    /**
     * Starts the Discord <-> SVC audio transfer system.
     */
    @SuppressWarnings("DataFlowIssue")
    public void start() {
        if (!hasLoggedIn) {
            platform.error("Tried to start audio transfer system but the bot has not been logged into. The bot may have failed to login.");
            return;
        }
        platform.debug("starting bot with vc_id " + vcId);

        VoiceChannel channel = jda.getChannelById(VoiceChannel.class, vcId);
        if (channel == null) {
            platform.error(
                    "Please ensure that all voice channel IDs are valid, available to the bot and that they are actual voice channels.");
            platform.sendMessage(
                    player,
                    "§cThe provided voice channel ID seems to be invalid or inaccessible to the  Please make sure that it is available to the bot and that it is an actual voice channel."
            );
            return;
        }

        Guild guild = channel.getGuild();
        manager = guild.getAudioManager();

        manager.setSendingHandler(this);
        manager.setReceivingHandler(this);
        manager.openAudioConnection(channel);

        discordEncoder = api.createEncoder();

        listener = api.playerAudioListenerBuilder()
                .setPacketListener(this::handleOutgoingSoundPacket)
                .setPlayer(player.getUuid())
                .build();
        api.registerAudioListener(listener);

        sender = api.createAudioSender(api.getConnectionOf(player));
        if (!api.registerAudioSender(sender)) {
            platform.error("Couldn't register audio sender. The player has the mod installed.");
            platform.sendMessage(
                    player,
                    "§cCouldn't register an audio sender for you. This most likely means you have the mod installed and working."
            );
            this.stop();
            return;
        }

        resetWatcher = new Thread(() -> {
            while (true) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    platform.debug("exiting reset watcher thread");
                    break;
                }

                if (lastTimeAudioProvidedToSVC != 0 && System.currentTimeMillis() - 100 > lastTimeAudioProvidedToSVC) {
                    platform.debugVerbose("resetting sender for player with UUID " + player.getUuid());
                    sender.reset();
                    lastTimeAudioProvidedToSVC = 0;
                }

                if (lastTimeAudioProvidedToDiscord != 0 && System.currentTimeMillis() - 100 > lastTimeAudioProvidedToDiscord) {
                    platform.debugVerbose("resetting encoder for player with UUID " + player.getUuid());
                    discordEncoder.resetState();
                    lastTimeAudioProvidedToDiscord = 0;
                }

                for (Map.Entry<UUID, AudioSource> entry : audioSources.entrySet()) {
                    AudioSource source = entry.getValue();
                    if (source.lastTimeAudioReceived != 0 && System.currentTimeMillis() - 100 > source.lastTimeAudioReceived) {
                        platform.debugVerbose("resetting decoder for source with UUID " + entry.getKey());
                        source.decoder.resetState();
                        source.lastTimeAudioReceived = 0;
                    }
                }
            }
        });
        resetWatcher.start();

        api.getConnectionOf(player).setConnected(true);

        String channelName = channel.getName();
        platform.info("Started voice chat for " + platform.getName(player) + " in channel " + channelName);
        platform.sendMessage(
                player,
                "§aStarted a voice chat! To stop it, use §r§f/dvc stop§r§a. If you are having issues, try restarting the session with §r§f/dvc start§r§a. Please join the following voice channel in discord: §r§f" + channelName
        );
    }

    /**
     * Stops the Discord <-> SVC audio transfer system and clears all queued audio. Also tries to remove almost everything from memory
     */
    @SuppressWarnings("DataFlowIssue")
    public void stop() {
        platform.debug("stopping bot with vc_id " + vcId);

        lastTimeAudioProvidedToSVC = 0;
        lastTimeAudioProvidedToDiscord = 0;

        if (manager != null) {
            manager.setSendingHandler(null);
            manager.setReceivingHandler(null);
            manager.closeAudioConnection();
            manager = null;
        }

        if (player != null) {
            api.getConnectionOf(player).setConnected(false);
            player = null;
        }

        if (listener != null) {
            api.unregisterAudioListener(listener);
            listener = null;
        }

        if (sender != null) {
            sender.reset();
            api.unregisterAudioSender(sender);
            sender = null;
        }

        if (resetWatcher != null) {
            resetWatcher.interrupt();
            resetWatcher = null;
        }

        if (discordEncoder != null) {
            discordEncoder.close();
            discordEncoder = null;
        }

        for (AudioSource player : audioSources.values()) {
            player.outgoingAudio.clear();
            player.decoder.close();
        }
        audioSources.clear();
    }

    private AudioSource getAudioSource(UUID sourceId) {
        platform.debugExtremelyVerbose("getting player data for " + sourceId);
        AudioSource data = audioSources.get(sourceId);
        if (data == null) {
            data = new AudioSource();
            audioSources.put(sourceId, data);
        }
        return data;
    }

    // === OUTGOING ===

    /**
     * Handles packets that will go to discord.
     */
    private void handleOutgoingSoundPacket(SoundPacket packet) {
        @Nullable Position position = null;
        float distance = 0.0f;
        UUID sender = packet.getSender();
        short[] audio = getAudioSource(sender).decoder.decode(packet.getOpusEncodedData());

        platform.debugExtremelyVerbose("outgoing packet is a " + packet.getClass().getSimpleName());
        if (packet instanceof LocationalSoundPacket sound) {
            position = sound.getPosition();
            distance = sound.getDistance();
        } else if (packet instanceof EntitySoundPacket sound) {
            platform.getEntityPosition(player.getServerLevel(), sound.getEntityUuid()).thenAccept(position1 -> handleOutgoingSound(audio, sender, sound.getDistance(), position1));
            return;
        } else if (!(packet instanceof StaticSoundPacket)) {
            platform.error("packet is not LocationalSoundPacket, StaticSoundPacket or EntitySoundPacket, it is " + packet.getClass().getSimpleName() + ". Please report this on GitHub Issues!");
        }

        handleOutgoingSound(audio, sender, distance, position);
    }

    /**
     * Handles packets that will go to discord, after the packet being normalized into the raw audio, sender, distance and position.
     */
    private void handleOutgoingSound(short[] audio, UUID sender, double distance, @Nullable Position position) {
        if (position != null) {
            audio = AudioCore.adjustVolumeBasedOnDistance(audio, position, this.player.getPosition(), distance);
        }

        platform.debugExtremelyVerbose("adding outgoing audio for " + sender + " (length of audio is " + audio.length + ")");
        AudioSource source = getAudioSource(sender);
        Queue<Short> outgoingAudio = source.outgoingAudio;
        for (short data : audio) {
            outgoingAudio.add(data);
        }
    }

    /**
     * Returns audio which will be played by the Discord bot
     */
    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        if (!canProvide()) {
            return null;
        }
        lastTimeAudioProvidedToDiscord = System.currentTimeMillis();
        return ByteBuffer.wrap(discordEncoder.encode(pollOutgoingAudio()));
    }

    /**
     * Provides 20ms of audio that will be played by the Discord bot
     */
    private short[] pollOutgoingAudio() {
        List<short[]> audioParts = new LinkedList<>();

        audioSources.values().forEach(source -> {
            Queue<Short> outgoingAudio = source.outgoingAudio;
            short[] audioPart = new short[AudioCore.SHORTS_IN_20MS];
            for (int i = 0; i < AudioCore.SHORTS_IN_20MS; i++) {
                if (outgoingAudio.isEmpty()) {
                    platform.debugVerbose("outgoingAudio is empty, we were able to get " + i + " short");
                    break;
                }
                audioPart[i] = outgoingAudio.poll();
            }
            audioParts.add(audioPart);
        });

        if (audioParts.size() > 1) {
            platform.debugExtremelyVerbose("combining " + audioParts.size() + " audio parts");
            return AudioCore.combineAudioParts(audioParts);
        } else {
            platform.debugExtremelyVerbose("not combining audio parts");
            return audioParts.get(0);
        }
    }

    /**
     * Returns if the bot can provide audio to Discord.
     */
    @Override
    public boolean canProvide() {
        return audioSources.values().stream().anyMatch(source -> !source.outgoingAudio.isEmpty());
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    // === INCOMING ===

    /**
     * Takes in audio which was heard by the Discord
     */
    @Override
    public void handleEncodedAudio(@NotNull OpusPacket packet) {
        platform.debugExtremelyVerbose("sending audio to SVC from player with UUID " + player.getUuid());
        lastTimeAudioProvidedToSVC = System.currentTimeMillis();
        sender.send(packet.getOpusAudio());
    }

    @Override
    public boolean canReceiveEncoded() {
        return true;
    }

    private static class AudioSource {
        public final Queue<Short> outgoingAudio = new ConcurrentLinkedQueue<>();
        public final OpusDecoder decoder = api.createDecoder();
        /**
         * The last time (unix timestamp) that audio has been added to outgoingAudio
         */
        public long lastTimeAudioReceived = 0;
    }
}
