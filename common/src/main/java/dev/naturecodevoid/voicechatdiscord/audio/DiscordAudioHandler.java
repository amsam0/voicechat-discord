package dev.naturecodevoid.voicechatdiscord.audio;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.SoundPacket;
import dev.naturecodevoid.voicechatdiscord.Platform;
import dev.naturecodevoid.voicechatdiscord.DiscordBot;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.OpusPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static dev.naturecodevoid.voicechatdiscord.Common.platform;


// Handler for transferring data between Discord and SVC.
public class DiscordAudioHandler implements AudioSendHandler, AudioReceiveHandler {

    // The Discord bot handler.
    private final DiscordBot bot;
    //private boolean hasRefreshedEncoder = false;
    //private boolean hasRefreshedDecoder = false;

    public DiscordAudioHandler(DiscordBot bot) {
        this.bot = bot;
    }



    // === OUTGOING ===

    public record Pair<A, B>(A a, B b) {}
    public record Triplet<A, B, C>(A a, B b, C c) {}
    private final List<Pair<CompletableFuture<Platform.EntityData>, Triplet<Float, UUID, short[]>>> pendingOutgoingAudio = new ArrayList<>();

    // Takes in audio which was heard by the SVC listener.
    public void handleOutgoingSoundPacket(SoundPacket packet) {

        @Nullable Position position = null;
        float              distance = 0.0f;
        UUID               sender   = packet.getSender();
        short[]            audio    = bot.audioBridge.getOutgoingDecoder(sender).decode(packet.getOpusEncodedData());

        if (packet instanceof LocationalSoundPacket sound) {
            position = sound.getPosition();
            distance = sound.getDistance();
        } else if (packet instanceof EntitySoundPacket sound) {
            // Don't even ask. This is BukkitMC synchronisation for ya.
            // Though this is still run on FabricMC, it is effectively unused due to the future already eing completed when its returned.
            CompletableFuture<Platform.EntityData> future = platform.getEntityData(bot.player.getServerLevel(), sound.getEntityUuid());
            pendingOutgoingAudio.add(new Pair<>(future, new Triplet<>(sound.getDistance(), sender, audio)));
            future.thenRun(() -> {
                synchronized (pendingOutgoingAudio) {
                    if (pendingOutgoingAudio.size() > 0) {
                        if (pendingOutgoingAudio.get(0).a == future) {
                            Iterator<Pair<CompletableFuture<Platform.EntityData>, Triplet<Float, UUID, short[]>>> iter = pendingOutgoingAudio.listIterator();
                            while (iter.hasNext()) {
                                Pair<CompletableFuture<Platform.EntityData>, Triplet<Float, UUID, short[]>> data = iter.next();
                                if (data.a.isDone()) {
                                    try {
                                        Platform.EntityData entity = data.a.get();
                                        if (entity != null) {
                                            handleOutgoingSound(data.b.c, data.b.b, data.b.a, entity.position());
                                        }
                                    } catch (InterruptedException | ExecutionException ignored) {}
                                } else if (! data.a.isCancelled()) {
                                    break;
                                }
                                iter.remove();
                            }
                        }
                    }
                }
            });
            return;
        }

        handleOutgoingSound(audio, sender, distance, position);

    }
    public void handleOutgoingSound(short[] audio, UUID sender, double distance, @Nullable Position position) {
        if (position != null) {
            audio = AudioCore.adjustVolumeOfOpusDecodedAudio(audio, position, this.bot.player.getPosition(), distance);
        }
        bot.audioBridge.addOutgoingAudio(sender, audio);
    }

    // Returns whether #provide20MsAudio will return anything.
    @Override
    public boolean canProvide() {
        return bot.audioBridge.hasOutgoingAudio();
    }

    // Returns whether #provide20MsAudio will return Opus-encoded audio.
    @Override
    public boolean isOpus() {
        return true;
    }

    // Returns audio which will be played by the Discord bot.
    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        if (! bot.audioBridge.hasOutgoingAudio()) {
            return null;
        }
        return ByteBuffer.wrap(bot.discordEncoder.encode(bot.audioBridge.pollOutgoingAudio()));
    }



    // === INCOMING ===

    // Takes in audio which was heard by the Discord bot.
    @Override
    public void handleEncodedAudio(@NotNull OpusPacket packet) {
        // invalid discord audio may cause the audio player thread to crash, so recreate it if it does
        // or at least, we think that's what happens... ¯\_(ツ)_/¯
        if (bot.audioPlayer.isStopped()) {
            platform.info("An audio player seems to have crashed, recreating it");
            bot.createAudioPlayer();
        }
        short[] audio = bot.discordDecoder.decode(packet.getOpusAudio());
        bot.audioBridge.addIncomingMicrophoneAudio(audio);

    }

    // Returns whether #provide20MsIncomingAudio will return anything.
    @Override
    public boolean canReceiveEncoded() {
        return true;
    }

    // Returns audio which will be played by the SVC player.
    public short[] provide20MsIncomingAudio() {
        return bot.audioBridge.pollIncomingAudio();
    }

}
