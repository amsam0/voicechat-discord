package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import dev.naturecodevoid.voicechatdiscord.listeners.PlayerLeave;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public final class BukkitPlugin extends JavaPlugin {
    public static final String PLUGIN_ID = "voicechat-discord";
    public static final Logger LOGGER = LogManager.getLogger(PLUGIN_ID);
    public static final ArrayList<Bot> bots = new ArrayList<>();
    public static VoicechatServerApi api;
    @Nullable
    private VoicechatPlugin voicechatPlugin;

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

    @SuppressWarnings({"DataFlowIssue", "unchecked"})
    @Override
    public void onEnable() {
        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            voicechatPlugin = new VoicechatPlugin();
            service.registerPlugin(voicechatPlugin);
            LOGGER.info("Successfully registered voicechat discord plugin");
        } else {
            LOGGER.info("Failed to register voicechat discord plugin");
        }

        getCommand("startdiscordvoicechat").setExecutor(new StartVoicechatCommand());
        Bukkit.getPluginManager().registerEvents(new PlayerLeave(), this);

        LinkedHashMap<String, String> defaultBot = new LinkedHashMap<>();
        defaultBot.put("token", "DISCORD_BOT_TOKEN_HERE");
        defaultBot.put("vc_id", "VOICE_CHANNEL_ID_HERE");
        getConfig().addDefault("bots", new ArrayList<>(List.of(defaultBot)));

        getConfig().options().copyDefaults(true);
        getConfig().options().setHeader(List.of(
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
                "For more information on getting everything setup: https://modrinth.com/mod/simple-voice-chat-discord"
        ));
        saveConfig();

        for (LinkedHashMap<String, Object> bot : (ArrayList<LinkedHashMap<String, Object>>) getConfig().getList("bots")) {
            try {
                bots.add(new Bot((String) bot.get("token"), (Long) bot.get("vc_id")));
            } catch (ClassCastException e) {
                LOGGER.fatal("Failed to load a bot. Please make sure that the vc_id property is a valid channel ID.");
            }
        }

        LOGGER.info("Using " + bots.size() + " bot" + (bots.size() != 1 ? "s" : ""));
    }

    @Override
    public void onDisable() {
        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);
            LOGGER.info("Successfully unregistered voicechat discord plugin");
        }
    }
}
