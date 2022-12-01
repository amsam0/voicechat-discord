package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import okhttp3.OkHttpClient;
import org.bspfsystems.yamlconfiguration.configuration.InvalidConfigurationException;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class VoicechatDiscord {
    public static final String PLUGIN_ID = "voicechat-discord";
    public static final ArrayList<Bot> bots = new ArrayList<>();
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
    public static VoicechatServerApi api;
    public static Platform platform;
    public static YamlConfiguration config;

    @SuppressWarnings({"DataFlowIssue", "unchecked"})
    public static void enable() {
        File configFile = new File(platform.getConfigPath());
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

        for (Bot bot : bots) {
            bot.stop();
            bot.jda.shutdownNow();
            OkHttpClient client = bot.jda.getHttpClient();
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdownNow();
        }

        platform.info("Successfully shutdown " + bots.size() + " bot" + (bots.size() != 1 ? "s" : ""));
    }

    /**
     * @param sender Should be a CommandSender or net.minecraft.server.network.ServerPlayerEntity
     */
    public static void runStartCommand(Object sender) {
        if (!platform.isValidPlayer(sender)) {
            platform.sendMessage(sender, "§cYou must be a player to use this command!");
            return;
        }

        ServerPlayer player = api.fromServerPlayer(sender);

        if (getBotForPlayer(player.getUuid()) != null) {
            platform.sendMessage(player, "§cYou have already started a voice chat!");
            return;
        }

        Bot bot = getAvailableBot();

        if (bot == null) {
            platform.sendMessage(
                    player,
                    "§cThere are currently no bots available. You might want to contact your server owner to add more."
            );
            return;
        }

        platform.sendMessage(player, "§eStarting a voice chat...");

        new Thread(() -> {
            bot.login();
            bot.start(player);
        }).start();
    }

    public static void onPlayerLeave(UUID playerUuid) {
        Bot bot = getBotForPlayer(playerUuid);
        if (bot != null)
            bot.stop();
    }

    public static Bot getBotForPlayer(UUID playerUuid) {
        for (Bot bot : bots) {
            if (bot.player != null)
                if (bot.player.getUuid().compareTo(playerUuid) == 0)
                    return bot;
        }
        return null;
    }

    public static Bot getAvailableBot() {
        for (Bot bot : bots) {
            if (bot.player == null)
                return bot;
        }
        return null;
    }
}
