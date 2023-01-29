package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import okhttp3.OkHttpClient;
import org.bspfsystems.yamlconfiguration.configuration.InvalidConfigurationException;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Common {
    public static final String PLUGIN_ID = "voicechat-discord";
    public static final String RELOAD_CONFIG_PERMISSION = "voicechat-discord.reload-config";
    public static final List<String> configHeader = List.of(
            "To add a bot, just copy paste the following into bots:",
            "",
            "bots:",
            "- token: DISCORD_BOT_TOKEN_HERE",
            "  vc_id: VOICE_CHANNEL_ID_HERE",
            "",
            "Example for 2 bots:",
            "",
            "bots:",
            "- token: MyFirstBotsToken",
            "  vc_id: 1234567890123456789",
            "- token: MySecondBotsToken",
            "  vc_id: 9876543210987654321",
            "",
            "If you are only using 1 bot, just replace DISCORD_BOT_TOKEN_HERE with your bot's token and replace VOICE_CHANNEL_ID_HERE with the voice channel ID.",
            "",
            "For more information on getting everything setup: https://github.com/naturecodevoid/voicechat-discord#readme"
    );
    public static final ArrayList<Commands.Command> commands = new ArrayList<>();
    public static ArrayList<Bot> bots = new ArrayList<>();
    public static VoicechatServerApi api;
    public static Platform platform;
    public static YamlConfiguration config;
    public static HashMap<UUID, OpusDecoder> playerDecoders = new HashMap<>();

    public static void enable() {
        loadConfig();
        Commands.registerCommands();
    }

    @SuppressWarnings({"DataFlowIssue", "unchecked", "ResultOfMethodCallIgnored"})
    protected static void loadConfig() {
        File configFile = new File(platform.getConfigPath());

        if (!configFile.getParentFile().exists())
            configFile.getParentFile().mkdirs();

        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException ignored) {
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }

        LinkedHashMap<String, String> defaultBot = new LinkedHashMap<>();
        defaultBot.put("token", "DISCORD_BOT_TOKEN_HERE");
        defaultBot.put("vc_id", "VOICE_CHANNEL_ID_HERE");
        config.addDefault("bots", List.of(defaultBot));

        config.getOptions().setCopyDefaults(true);
        config.getOptions().setHeader(configHeader);
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (bots.size() > 0)
            bots = new ArrayList<>();

        for (LinkedHashMap<String, Object> bot : (List<LinkedHashMap<String, Object>>) config.getList("bots")) {
            try {
                bots.add(new Bot((String) bot.get("token"), (Long) bot.get("vc_id")));
            } catch (ClassCastException e) {
                platform.error(
                        "Failed to load a bot. Please make sure that the vc_id property is a valid channel ID.");
            }
        }

        platform.info("Using " + bots.size() + " bot" + (bots.size() != 1 ? "s" : ""));
    }

    public static void disable() {
        platform.info("Shutting down " + bots.size() + " bot" + (bots.size() != 1 ? "s" : ""));

        stopBots();

        for (OpusDecoder decoder : playerDecoders.values())
            decoder.close();

        playerDecoders = null;

        platform.info("Successfully shutdown " + bots.size() + " bot" + (bots.size() != 1 ? "s" : ""));
    }

    protected static void stopBots() {
        for (Bot bot : bots) {
            bot.stop();
            if (bot.jda == null)
                continue;
            bot.jda.shutdownNow();
            OkHttpClient client = bot.jda.getHttpClient();
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdownNow();
        }
    }

    public static void onPlayerLeave(UUID playerUuid) {
        Bot bot = getBotForPlayer(playerUuid);
        if (bot != null) {
            platform.info("Stopping bot");
            bot.stop();
        }
    }

    public static void afterPlayerRespawn(ServerPlayer newPlayer) {
        Bot bot = getBotForPlayer(newPlayer.getUuid());
        if (bot != null)
            bot.audioChannel.updateEntity(newPlayer);
    }

    public static Bot getBotForPlayer(UUID playerUuid) {
        return getBotForPlayer(playerUuid, false);
    }

    public static Bot getBotForPlayer(UUID playerUuid, boolean fallbackToAvailableBot) {
        for (Bot bot : bots) {
            if (bot.player != null)
                if (bot.player.getUuid().compareTo(playerUuid) == 0)
                    return bot;
        }
        if (fallbackToAvailableBot)
            return getAvailableBot();
        return null;
    }

    public static Bot getAvailableBot() {
        for (Bot bot : bots) {
            if (bot.player == null)
                return bot;
        }
        return null;
    }

    public static OpusDecoder getPlayerDecoder(UUID playerUuid) {
        OpusDecoder decoder = playerDecoders.get(playerUuid);
        if (decoder == null) {
            decoder = api.createDecoder();
            playerDecoders.put(playerUuid, decoder);
        }
        return decoder;
    }
}
