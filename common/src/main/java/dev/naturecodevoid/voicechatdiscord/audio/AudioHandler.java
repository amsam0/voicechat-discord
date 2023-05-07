package dev.naturecodevoid.voicechatdiscord.audio;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.packets.*;
import dev.naturecodevoid.voicechatdiscord.Bot;
import dev.naturecodevoid.voicechatdiscord.Platform;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.OpusPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.UUID;

import static dev.naturecodevoid.voicechatdiscord.Common.platform;


// Handler for transferring data between Discord and SVC.
public class AudioHandler implements AudioSendHandler, AudioReceiveHandler {

    // The Discord bot handler.
    private final Bot bot;
    //private boolean hasRefreshedEncoder = false;
    //private boolean hasRefreshedDecoder = false;

    public AudioHandler(Bot bot) {
        this.bot = bot;
    }


    // === OUTGOING ===

    // Takes in audio which was heard by the SVC listener.
    public void handleOutgoingSoundPacket(SoundPacket packet) {

        @Nullable Position position = null;
        double             distance = 0.0;
        UUID               sender   = packet.getSender();

        if (packet instanceof LocationalSoundPacket sound) {
            position = sound.getPosition();
            distance = sound.getDistance();
        } else if (packet instanceof EntitySoundPacket sound) {
            Platform.EntityData entity = platform.getEntityData(bot.player.getServerLevel(), sound.getEntityUuid());
            if (entity == null || entity.uuid().equals(bot.player.getUuid())) {return;}
            position = entity.position();
            distance = sound.getDistance();
        }

        short[] audio = bot.audioBridge.getOutgoingDecoder(sender).decode(packet.getOpusEncodedData());
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
            /*if (!hasRefreshedEncoder) {
                bot.discordEncoder.close();
                bot.discordEncoder = api.createEncoder();
                hasRefreshedEncoder = true;
            }*/
            return null;
        }
        //hasRefreshedEncoder = false;

        return ByteBuffer.wrap(bot.opusEncoder.encode(bot.audioBridge.pollOutgoingAudio()));
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

        short[] audio = bot.opusDecoder.decode(packet.getOpusAudio());
        bot.audioBridge.addIncomingMicrophoneAudio(audio);
    }

    // Returns whether this can transfer audio from Discord to SVC.
    @Override
    public boolean canReceiveEncoded() {
        return true;
    }

    // Returns audio which will be played by the SVC player.
    public short[] provide20MsIncomingAudio() {
        return bot.audioBridge.pollIncomingAudio();
    }

}
