package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import okhttp3.OkHttpClient;
import org.bspfsystems.yamlconfiguration.configuration.InvalidConfigurationException;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Common code between Paper and Fabric.
 */
public class Common {
    public static final String PLUGIN_ID = "voicechat-discord";
    public static final String RELOAD_CONFIG_PERMISSION = "voicechat-discord.reload-config";
    public static final String VOICECHAT_MIN_VERSION = "2.4.8";
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
            "If you are reporting an issue or trying to figure out what's causing an issue, you may find the `debug_level` option helpful.",
            "It will enable debug logging according to the level:",
            "- 0 (or lower): No debug logging",
            "- 1: Some debug logging (mainly logging that won't spam the console but can be helpful)",
            "- 2: Most debug logging (will spam the console but excludes logging that is extremely verbose and usually not helpful)",
            "- 3 (or higher): All debug logging (will spam the console)",
            "",
            "For more information on getting everything setup: https://github.com/naturecodevoid/voicechat-discord#readme"
    );
    public static final ArrayList<SubCommands.SubCommand> SUB_COMMANDS = new ArrayList<>();
    public static ArrayList<DiscordBot> bots = new ArrayList<>();
    public static VoicechatServerApi api;
    public static Platform platform;
    public static YamlConfiguration config;
    public static int debugLevel = 0;

    public static void enable() {
        loadConfig();
        SubCommands.register();
    }

    @SuppressWarnings({"DataFlowIssue", "unchecked", "ResultOfMethodCallIgnored"})
    protected static void loadConfig() {
        File configFile = new File(platform.getConfigPath());

        if (!configFile.getParentFile().exists())
            configFile.getParentFile().mkdirs();

        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException e) {
            platform.debug("IOException when loading config: " + e);
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }

        LinkedHashMap<String, String> defaultBot = new LinkedHashMap<>();
        defaultBot.put("token", "DISCORD_BOT_TOKEN_HERE");
        defaultBot.put("vc_id", "VOICE_CHANNEL_ID_HERE");
        config.addDefault("bots", List.of(defaultBot));

        config.addDefault("debug_level", 0);

        config.getOptions().setCopyDefaults(true);
        config.getOptions().setHeader(configHeader);
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!bots.isEmpty())
            bots = new ArrayList<>();

        for (LinkedHashMap<String, Object> bot : (List<LinkedHashMap<String, Object>>) config.getList("bots")) {
            try {
                bots.add(new DiscordBot((String) bot.get("token"), (Long) bot.get("vc_id")));
            } catch (ClassCastException e) {
                platform.error(
                        "Failed to load a bot. Please make sure that the vc_id property is a valid channel ID.");
            }
        }

        platform.info("Using " + bots.size() + " bot" + (bots.size() != 1 ? "s" : ""));

        try {
            debugLevel = (int) config.get("debug_level");
            if (debugLevel > 0) platform.info("Debug mode has been set to level " + debugLevel);
        } catch (ClassCastException e) {
            platform.error("Please make sure the debug option is a valid integer");
        }
    }

    public static void disable() {
        platform.info("Shutting down " + bots.size() + " bot" + (bots.size() != 1 ? "s" : ""));

        stopBots();

        platform.info("Successfully shutdown " + bots.size() + " bot" + (bots.size() != 1 ? "s" : ""));
    }

    protected static void stopBots() {
        for (DiscordBot bot : bots) {
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
        DiscordBot bot = getBotForPlayer(playerUuid);
        if (bot != null) {
            platform.info("Stopping bot");
            bot.stop();
        }
    }

    public static void afterPlayerRespawn(ServerPlayer newPlayer) {
        DiscordBot bot = getBotForPlayer(newPlayer.getUuid());
        if (bot != null) {
            platform.debug("updating bot for player with UUID " + newPlayer.getUuid());
            bot.audioChannel.updateEntity(newPlayer);
        }
    }

    public static DiscordBot getBotForPlayer(UUID playerUuid) {
        return getBotForPlayer(playerUuid, false);
    }

    public static DiscordBot getBotForPlayer(UUID playerUuid, boolean fallbackToAvailableBot) {
        for (DiscordBot bot : bots) {
            if (bot.player != null)
                if (bot.player.getUuid().compareTo(playerUuid) == 0)
                    return bot;
        }
        if (fallbackToAvailableBot)
            return getAvailableBot();
        return null;
    }

    public static DiscordBot getAvailableBot() {
        for (DiscordBot bot : bots) {
            if (bot.player == null)
                return bot;
        }
        return null;
    }

    private static int @Nullable [] splitVersion(String version) {
        try {
            return Arrays.stream(version.split("\\."))
                    // if there is a -pre we need to remove it
                    .limit(3)
                    .map(str -> str.split("-")[0])
                    .mapToInt(Integer::parseInt)
                    .toArray();
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean isSVCVersionSufficient(String version) {
        String[] splitVersion = version.split("-");
        int[] parsedVersion = splitVersion(splitVersion[splitVersion.length - 1]);
        platform.debug("parsed version: " + Arrays.toString(parsedVersion));
        int[] parsedMinVersion = Objects.requireNonNull(splitVersion(VOICECHAT_MIN_VERSION));
        platform.debug("parsed min version: " + Arrays.toString(parsedMinVersion));
        if (parsedVersion != null) {
            for (int i = 0; i < parsedMinVersion.length; i++) {
                int part = parsedMinVersion[i];
                int testPart;
                if (parsedVersion.length > i) {
                    testPart = parsedVersion[i];
                } else {
                    testPart = 0;
                }
                if (testPart < part) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns true if the SVC version is not new enough
     */
    public static void checkSVCVersion(@Nullable String version) {
        if (version == null || !isSVCVersionSufficient(version)) {
            String message = "Simple Voice Chat Discord Bridge requires Simple Voice Chat version " + VOICECHAT_MIN_VERSION + " or later";
            if (version != null) {
                message += " You have version " + version + ".";
            }
            platform.error(message);
            throw new RuntimeException(message);
        }
    }
}
