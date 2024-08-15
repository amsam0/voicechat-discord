package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.audiolistener.AudioListener;
import de.maxhenkel.voicechat.api.audiosender.AudioSender;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.SoundPacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import dev.naturecodevoid.voicechatdiscord.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static dev.naturecodevoid.voicechatdiscord.Core.api;
import static dev.naturecodevoid.voicechatdiscord.Core.platform;

public class DiscordBot {
    // Make sure to mirror this value on the Rust side (`DiscordBot::reset_senders::DURATION_UNTIL_RESET`)
    private static final int MILLISECONDS_UNTIL_RESET = 1000;
    /**
     * ID for the voice channel the bot is assigned to
     */
    private final long vcId;
    /**
     * Pointer to Rust struct
     */
    private final long ptr;
    /**
     * The player that this Discord bot is linked to.
     */
    private ServerPlayer player;
    /**
     * The SVC audio sender used to send audio to SVC.
     */
    private AudioSender sender;
    /**
     * A thread that sends opus data to the AudioSender.
     */
    private Thread senderThread;
    /**
     * The last time (unix timestamp) that audio was sent to the audio sender.
     */
    private Long lastTimeAudioProvidedToSVC;
    /**
     * A thread that checks every 500ms if the audio sender, discord encoder and audio source decoders should be reset.
     */
    private Thread resetThread;
    /**
     * The SVC audio listener to listen for outgoing (to Discord) audio.
     */
    private AudioListener listener;
    private int connectionNumber = 0;

    public @Nullable ServerPlayer player() {
        return player;
    }

    public boolean whispering() {
        return sender.isWhispering();
    }

    public void whispering(boolean set) {
        sender.whispering(set);
    }

    private static native long _new(String token, long vcId);

    public DiscordBot(String token, long vcId) {
        this.vcId = vcId;
        ptr = _new(token, vcId);
    }

    public void logInAndStart(ServerPlayer player) {
        this.player = player;
        if (logIn())
            start();
    }

    private native boolean _isStarted(long ptr);

    public boolean isStarted() {
        return _isStarted(ptr);
    }

    private native void _logIn(long ptr) throws Exception;

    private boolean logIn() {
        try {
            _logIn(ptr);
            platform.debug("Logged into the bot with vc_id " + vcId);
            return true;
        } catch (Exception e) {
            platform.error("Failed to login to the bot with vc_id " + vcId + ": " + e.getMessage());
            if (player != null) {
                platform.sendMessage(
                        player,
                        // The error message won't contain the token, but let's be safe and not show it to the player
                        "<red>Failed to login to the bot. Please contact your server owner and ask them to look at the console since they will be able to see the error message."
                );
                player = null;
            }
            return false;
        }
    }

    private native String _start(long ptr) throws Exception;

    private void start() {
        assert player != null;

        String vcName;
        try {
            vcName = _start(ptr);
        } catch (Exception e) {
            platform.error("Failed to start voice connection for bot with vc_id " + vcId + ": " + e.getMessage());
            platform.sendMessage(
                    player,
                    "<red>Failed to start voice connection. Please contact your server owner since they will be able to see the error message."
            );
            stop();
            return;
        }

        var connection = api.getConnectionOf(player);
        assert connection != null; // connection should only be null if the player is not connected to the server

        listener = api.playerAudioListenerBuilder()
                .setPacketListener(this::handlePacket)
                .setPlayer(player.getUuid())
                .build();
        api.registerAudioListener(listener);

        sender = api.createAudioSender(connection);
        if (!api.registerAudioSender(sender)) {
            platform.error("Couldn't register audio sender. The player has the mod installed.");
            platform.sendMessage(
                    player,
                    "<red>It seems that you have Simple Voice Chat installed on your client. To use the addon, you must not have Simple Voice Chat installed on your client."
            );
            stop();
            return;
        }

        connectionNumber++;

        resetThread = new Thread(() -> {
            var startConnectionNumber = connectionNumber;
            platform.debug("reset thread " + startConnectionNumber + " starting");
            while (true) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                    platform.debug("reset thread " + startConnectionNumber + " interrupted");
                    break;
                }

                // Check after sleeping instead of before sleeping
                if (sender == null || connectionNumber != startConnectionNumber) break;

                if (lastTimeAudioProvidedToSVC != null && System.currentTimeMillis() - MILLISECONDS_UNTIL_RESET > lastTimeAudioProvidedToSVC) {
                    platform.debugVerbose("resetting sender for player with UUID " + player.getUuid());
                    sender.reset();
                    lastTimeAudioProvidedToSVC = null;
                }

                _resetSenders(ptr);
            }
            platform.debug("reset thread " + startConnectionNumber + " ending");
        });
        resetThread.start();

        senderThread = new Thread(() -> {
            var startConnectionNumber = connectionNumber;
            platform.debug("sender thread " + startConnectionNumber + " starting");
            while (true) {
                var data = _blockForSpeakingBufferOpusData(ptr);

                // Check after blocking instead of before blocking
                if (sender == null || connectionNumber != startConnectionNumber) break;

                if (data.length > 0) {
                    sender.send(data);
                    // make sure this is after _blockForSpeakingBufferOpusData - we don't want the time before blocking
                    lastTimeAudioProvidedToSVC = System.currentTimeMillis();
                }
            }
            platform.debug("sender thread " + startConnectionNumber + " ending");
        });
        senderThread.start();

        connection.setConnected(true);

        platform.info("Started voice chat for " + platform.getName(player) + " in channel " + vcName + " with bot with vc_id " + vcId);
        platform.sendMessage(
                player,
                "<green>Started a voice chat! To stop it, use <white>/dvc stop<green>. If you are having issues, try restarting the session with <white>/dvc start<green>. Please join the following voice channel in discord: <white>" + vcName
        );
    }

    private native void _stop(long ptr) throws Exception;

    public void stop() {
        // Help the threads end
        connectionNumber++;

        if (listener != null) {
            api.unregisterAudioListener(listener);
            listener = null;
        }

        if (sender != null) {
            sender.reset();
            api.unregisterAudioSender(sender);
            sender = null;
        }

        lastTimeAudioProvidedToSVC = null;
        if (resetThread != null) {
            resetThread.interrupt();
            while (resetThread != null && resetThread.isAlive()) {
                try {
                    platform.debug("waiting for reset thread to end");
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
            resetThread = null;
        }

        if (senderThread != null) {
            senderThread.interrupt(); // this really doesn't help stop the thread
            while (senderThread != null && senderThread.isAlive()) {
                try {
                    platform.debug("waiting for sender thread to end");
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
            senderThread = null;
        }

        // Threads are ended, so reset the connection number back to original (it will be incremented in start)
        // This way it doesn't jump from 1 to 3
        connectionNumber--;

        if (player != null) {
            var connection = api.getConnectionOf(player);
            // connection should only be null if the player is not connected to the server
            if (connection != null)
                connection.setConnected(false);
            player = null;
        }

        // Stop the rust side last so that the state is still Started for any received packets
        try {
            _stop(ptr);
        } catch (Exception e) {
            platform.warn("Failed to stop bot with vc_id " + vcId + ": " + e.getMessage());
        }
    }

    private native void _free(long ptr);

    /**
     * Safety: the class should be discarded after calling
     */
    public void free() {
        _free(ptr);
    }

    private native void _addAudioToHearingBuffer(long ptr, int senderId, byte[] rawOpusData, boolean adjustBasedOnDistance, double distance, double maxDistance);

    public void handlePacket(SoundPacket packet) {
        UUID senderId = packet.getSender();

        @Nullable Position position = null;
        double maxDistance = 0.0;
        boolean whispering = false;

        platform.debugExtremelyVerbose("packet is a " + packet.getClass().getSimpleName());
        if (packet instanceof EntitySoundPacket sound) {
            position = platform.getEntityPosition(player.getServerLevel(), sound.getEntityUuid());
            maxDistance = sound.getDistance();
            whispering = sound.isWhispering();
        } else if (packet instanceof LocationalSoundPacket sound) {
            position = sound.getPosition();
            maxDistance = sound.getDistance();
        } else if (!(packet instanceof StaticSoundPacket)) {
            platform.warn("packet is not LocationalSoundPacket, StaticSoundPacket or EntitySoundPacket, it is " + packet.getClass().getSimpleName() + ". Please report this on GitHub Issues!");
        }

        if (whispering) {
            platform.debugExtremelyVerbose("player is whispering, original max distance is " + maxDistance);
            maxDistance *= api.getServerConfig().getDouble("whisper_distance_multiplier", 1);
        }

        double distance = position != null
                ? Util.distance(position, player.getPosition())
                : 0.0;

        platform.debugExtremelyVerbose("adding audio for " + senderId);

        _addAudioToHearingBuffer(ptr, senderId.hashCode(), packet.getOpusEncodedData(), position != null, distance, maxDistance);
    }

    private native byte[] _blockForSpeakingBufferOpusData(long ptr);

    private native void _resetSenders(long ptr);
}