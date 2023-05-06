package dev.naturecodevoid.voicechatdiscord.audio;

import de.maxhenkel.voicechat.api.packets.*;
import dev.naturecodevoid.voicechatdiscord.Bot;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.OpusPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.UUID;

import static dev.naturecodevoid.voicechatdiscord.Common.api;
import static dev.naturecodevoid.voicechatdiscord.Common.platform;
import static dev.naturecodevoid.voicechatdiscord.audio.AudioCore.addAudioToBotsInRange;

public class AudioHandler implements AudioSendHandler, AudioReceiveHandler {

    private final Bot bot;
//    private boolean hasRefreshedEncoder = false;
//    private boolean hasRefreshedDecoder = false;

    public AudioHandler(Bot bot) {
        this.bot = bot;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        if (! bot.audioBridge.hasOutgoingAudio()) {
//            if (!hasRefreshedEncoder) {
//                bot.discordEncoder.close();
//                bot.discordEncoder = api.createEncoder();
//                hasRefreshedEncoder = true;
//            }
            return null;
        }

//        hasRefreshedEncoder = false;

        return ByteBuffer.wrap(bot.discordEncoder.encode(bot.audioBridge.pollOutgoingAudio()));
    }

    public short[] provide20MsIncomingAudio() {
        return bot.audioBridge.pollIncomingAudio();
    }

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

        addAudioToBotsInRange(
                bot.player,
                audio
        );
    }

    public void handleOutgoingSoundPacket(SoundPacket packet) {

        /*platform.warn(packet instanceof SoundPacket ? "true" : "false");
        platform.warn(packet instanceof StaticSoundPacket ? "true" : "false");
        platform.warn(packet instanceof LocationalSoundPacket ? "true" : "false");
        platform.warn(packet instanceof EntitySoundPacket ? "true" : "false");
        platform.warn(packet instanceof MicrophonePacket ? "true" : "false");
        platform.warn(packet.getClass().getSimpleName());*/
        // Printing true, false, false, false, false, de.maxhenkel.voicechat.plugins.impl.packets.SoundPacketImpl.
        // This is an issue with the SVC API. Should be fixed soon.

        byte[] audio = packet.getOpusEncodedData();
        // TODO : If it's locational, entity, or microphone, calculate volume here.
        short[] decodedAudio = bot.discordDecoder.decode(audio);
        if (false) { // TODO : // If the audio is from the microphone of a client who has SVC installed, run this. Otherwise, run the else block.
            bot.audioBridge.addOutgoingMicrophoneAudio(packet.getSender(), decodedAudio);
        } else {
            bot.audioBridge.addOutgoingGenericAudio(decodedAudio);
        }
    }

    @Override
    public boolean canProvide() {
        return bot.audioBridge.hasOutgoingAudio();
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    @Override
    public boolean canReceiveEncoded() {
        return true;
    }
}
