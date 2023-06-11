package dev.naturecodevoid.voicechatdiscord.audio;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.SoundPacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import dev.naturecodevoid.voicechatdiscord.DiscordBot;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.OpusPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.UUID;

import static dev.naturecodevoid.voicechatdiscord.Common.platform;

/**
 * JDA handler for transferring data between Discord and SVC.
 */
public class DiscordAudioHandler implements AudioSendHandler, AudioReceiveHandler {
    /**
     * The Discord bot this handler is running for.
     */
    private final DiscordBot bot;

    public DiscordAudioHandler(DiscordBot bot) {
        this.bot = bot;
    }

    // === OUTGOING ===

    /**
     * Handles packets that will go to discord.
     */
    public void handleOutgoingSoundPacket(SoundPacket packet) {
        @Nullable Position position = null;
        float distance = 0.0f;
        UUID sender = packet.getSender();
        short[] audio = bot.audioBridge.getOutgoingDecoder(sender).decode(packet.getOpusEncodedData());

        platform.debugExtremelyVerbose("outgoing packet is a " + packet.getClass().getSimpleName());
        if (packet instanceof LocationalSoundPacket sound) {
            position = sound.getPosition();
            distance = sound.getDistance();
        } else if (packet instanceof EntitySoundPacket sound) {
            position = platform.getEntityPosition(bot.player.getServerLevel(), sound.getEntityUuid());
            distance = sound.getDistance();
        } else if (!(packet instanceof StaticSoundPacket)) {
            platform.error("packet is not LocationalSoundPacket, StaticSoundPacket or EntitySoundPacket, it is " + packet.getClass().getSimpleName() + ". Please report this on GitHub Issues!");
        }

        handleOutgoingSound(audio, sender, distance, position);
    }

    private void handleOutgoingSound(short[] audio, UUID sender, double distance, @Nullable Position position) {
        if (position != null) {
            audio = AudioCore.adjustVolumeBasedOnDistance(audio, position, this.bot.player.getPosition(), distance);
        }
        bot.audioBridge.addOutgoingAudio(sender, audio);
    }

    /**
     * Returns audio which will be played by the Discord bot.
     */
    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        if (!bot.audioBridge.hasOutgoingAudio()) {
            return null;
        }
        return ByteBuffer.wrap(bot.discordEncoder.encode(bot.audioBridge.pollOutgoingAudio()));
    }

    /**
     * Returns if the bot can provide audio to Discord.
     */
    @Override
    public boolean canProvide() {
        return bot.audioBridge.hasOutgoingAudio();
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    // === INCOMING ===

    /**
     * Takes in audio which was heard by the Discord bot.
     */
    @Override
    public void handleEncodedAudio(@NotNull OpusPacket packet) {
        platform.debugExtremelyVerbose("sending audio to SVC from player with UUID " + bot.player.getUuid());
        bot.sender.send(packet.getOpusAudio());
        bot.lastTimeAudioSent = System.currentTimeMillis();
    }

    @Override
    public boolean canReceiveEncoded() {
        return true;
    }
}
