package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import dev.naturecodevoid.voicechatdiscord.listeners.PlayerLeave;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

public final class BukkitPlugin extends JavaPlugin {
    public static final String PLUGIN_ID = "voicechat-discord";
    public static final Logger LOGGER = LogManager.getLogger(PLUGIN_ID);
    public static final HashMap<UUID, ConnectedPlayerData> connectedPlayers = new HashMap<>();
    public static JDA jda;
    public static long vcId;
    public static VoicechatServerApi api;
    public static OpusDecoder decoder;
    public static OpusEncoder encoder;

    @Nullable
    private VoicechatPlugin voicechatPlugin;

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

        getConfig().addDefault("token", "DISCORD_BOT_TOKEN_HERE");
        getConfig().addDefault("vc_id", "VOICE_CHANNEL_ID_HERE");

        getConfig().options().copyDefaults(true);
        saveConfig();

        vcId = (long) getConfig().get("vc_id");

        jda = JDABuilder.createDefault((String) getConfig().get("token")).enableCache(CacheFlag.VOICE_STATE).build();
    }

    @Override
    public void onDisable() {
        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);
            LOGGER.info("Successfully unregistered voicechat discord plugin");
        }
    }
}
