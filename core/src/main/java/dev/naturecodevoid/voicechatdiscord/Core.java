package dev.naturecodevoid.voicechatdiscord;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import org.bspfsystems.yamlconfiguration.configuration.InvalidConfigurationException;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static dev.naturecodevoid.voicechatdiscord.Constants.CONFIG_HEADER;
import static dev.naturecodevoid.voicechatdiscord.Constants.VOICECHAT_MIN_VERSION;

/**
 * Core code between Paper and Fabric.
 */
public final class Core {
    public static ArrayList<DiscordBot> bots = new ArrayList<>();
    public static VoicechatServerApi api;
    public static Platform platform;
    public static int debugLevel = 0;
    public static boolean alertOpsOfUpdates = true;

    private static native void initLogger();

    private static native void setDebugLevel(int debugLevel);

    private static native void shutdown();

    /**
     * IMPORTANT: Nothing that runs in this function should depend on SVC's API. We don't know if the SVC is new enough yet
     */
    public static void enable() {
        try {
            LibraryLoader.load("voicechat_discord");
        } catch (Exception e) {
            platform.error("Failed to load natives: " + e.getMessage());
            throw new RuntimeException(e);
        }
        initLogger();

        new Thread(UpdateChecker::checkForUpdate).start();
        loadConfig();
    }

    public static void disable() {
        int toShutdown = bots.size();
        platform.info("Shutting down " + toShutdown + " bot" + (toShutdown != 1 ? "s" : ""));

        clearBots();

        platform.info("Successfully shutdown " + toShutdown + " bot" + (toShutdown != 1 ? "s" : ""));

        shutdown();

        platform.info("Successfully shutdown native runtime");
    }

    @SuppressWarnings({"DataFlowIssue", "unchecked", "ResultOfMethodCallIgnored"})
    public static void loadConfig() {
        File configFile = new File(platform.getConfigPath());

        if (!configFile.getParentFile().exists())
            configFile.getParentFile().mkdirs();

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException e) {
            platform.warn("IOException when loading config: " + e.getMessage());
            platform.debug(e);
        } catch (InvalidConfigurationException e) {
            platform.error("Failed to load config file: " + e.getMessage());
            throw new RuntimeException(e);
        }

        LinkedHashMap<String, String> defaultBot = new LinkedHashMap<>();
        defaultBot.put("token", "DISCORD_BOT_TOKEN_HERE");
        defaultBot.put("vc_id", "VOICE_CHANNEL_ID_HERE");
        config.addDefault("bots", List.of(defaultBot));

        config.addDefault("alert_ops_of_updates", true);

        config.addDefault("debug_level", 0);

        config.getOptions().setCopyDefaults(true);
        config.getOptions().setHeader(CONFIG_HEADER);
        try {
            config.save(configFile);
        } catch (IOException e) {
            platform.error("Failed to save config file: " + e.getMessage());
            throw new RuntimeException(e);
        }

        bots.clear();

        for (LinkedHashMap<String, Object> bot : (List<LinkedHashMap<String, Object>>) config.getList("bots")) {
            if (bot.get("token") == null) {
                platform.error(
                        "Failed to load a bot, missing token property.");
                continue;
            }

            if (bot.get("vc_id") == null) {
                platform.error(
                        "Failed to load a bot, missing vc_id property.");
                continue;
            }

            try {
                bots.add(new DiscordBot((String) bot.get("token"), (Long) bot.get("vc_id")));
            } catch (ClassCastException e) {
                platform.error(
                        "Failed to load a bot. Please make sure that the token property is a string and the vc_id property is a number.");
            }
        }

        platform.info("Using " + bots.size() + " bot" + (bots.size() != 1 ? "s" : ""));

        try {
            alertOpsOfUpdates = (boolean) config.get("alert_ops_of_updates");
            if (!alertOpsOfUpdates)
                platform.info("Operators will not be alerted of new updates. Please make sure you check the console for new updates!");
        } catch (ClassCastException e) {
            platform.error("Please make sure the alert_ops_of_updates option is a valid boolean (true or false)");
        }

        try {
            debugLevel = (int) config.get("debug_level");
            if (debugLevel > 0) platform.info("Debug level has been set to " + debugLevel);
            setDebugLevel(debugLevel);
        } catch (ClassCastException e) {
            platform.error("Please make sure the debug_level option is a valid integer");
        }
    }

    public static void clearBots() {
        bots.forEach(discordBot -> {
            discordBot.stop();
            discordBot.free();
        });
        bots.clear();
    }

    public static void onPlayerJoin(Object rawPlayer) {
        if (UpdateChecker.updateMessage != null && platform.isOperator(rawPlayer)) {
            if (alertOpsOfUpdates) {
                platform.sendMessage(api.fromServerPlayer(rawPlayer), UpdateChecker.updateMessage);
                platform.debug("Alerted operator of new update");
            } else {
                platform.debug("Not alerting operator of new update");
            }
        }
    }

    public static void onPlayerLeave(UUID playerUuid) {
        DiscordBot bot = getBotForPlayer(playerUuid);
        if (bot != null) {
            platform.info("Stopping bot");
            bot.stop();
        }
    }

    public static @Nullable DiscordBot getBotForPlayer(UUID playerUuid) {
        return getBotForPlayer(playerUuid, false);
    }

    public static @Nullable DiscordBot getBotForPlayer(UUID playerUuid, boolean fallbackToAvailableBot) {
        for (DiscordBot bot : bots) {
            if (bot.player() != null && bot.player().getUuid() == playerUuid)
                return bot;
        }
        if (fallbackToAvailableBot)
            return getAvailableBot();
        return null;
    }

    private static @Nullable DiscordBot getAvailableBot() {
        for (DiscordBot bot : bots) {
            if (bot.player() == null)
                return bot;
        }
        return null;
    }

    public static void checkSVCVersion(@Nullable String version) {
        if (version != null) {
            platform.debug("SVC version: " + version);
            String[] splitVersion = version.split("-");
            if (splitVersion.length > 1) {
                // Beta builds are fine since they will have the new APIs we depend on.
                // If we don't remove the ending part, it will say SVC isn't new enough
                if (platform.getLoader() == Platform.Loader.FABRIC) {
                    // On fabric, the version is prefixed with the minecraft version
                    // We don't care about the minecraft version
                    version = splitVersion[1];
                } else {
                    // We're on Paper, we still want to get rid of the ending part (pre1)
                    version = splitVersion[0];
                }
                platform.debug("SVC version after normalizing: " + version);
            }
        }

        try {
            if (version == null || Version.parse(version).isLowerThan(Version.parse(VOICECHAT_MIN_VERSION))) {
                String message = "Simple Voice Chat Discord Bridge requires Simple Voice Chat version " + VOICECHAT_MIN_VERSION + " or later.";
                if (version != null) {
                    message += " You have version " + version + ".";
                }
                platform.error(message);
                throw new RuntimeException(message);
            }
        } catch (IllegalArgumentException | ParseException e) {
            platform.error("Failed to parse SVC version: " + e.getMessage());
            platform.debug(e);
            platform.warn("Assuming SVC is " + VOICECHAT_MIN_VERSION + " or later. If not, things will break.");
        }
    }
}
