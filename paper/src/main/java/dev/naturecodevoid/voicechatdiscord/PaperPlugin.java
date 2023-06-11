package dev.naturecodevoid.voicechatdiscord;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import net.minecraft.commands.CommandSourceStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;

import static dev.naturecodevoid.voicechatdiscord.Common.*;

public final class PaperPlugin extends JavaPlugin implements Listener {
    public static final Logger LOGGER = LogManager.getLogger(Constants.PLUGIN_ID);
    public static PaperPlugin INSTANCE;
    private VoicechatPlugin voicechatPlugin;

    public static PaperPlugin get() {
        return INSTANCE;
    }

    public static Class<?> getCraftServer() throws ClassNotFoundException {
        return Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".CraftServer");
    }

    public static Class<?> getVanillaCommandWrapper() throws ClassNotFoundException {
        return Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".command.VanillaCommandWrapper");
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        platform = new PaperPlatform();

        // Check if SVC is installed and is at least at the minimum version.
        Plugin svcPlugin = getServer().getPluginManager().getPlugin("voicechat");
        checkSVCVersion(svcPlugin != null ? svcPlugin.getDescription().getVersion() : null);

        // Setup the plugin.

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            voicechatPlugin = new VoicechatPlugin();
            service.registerPlugin(voicechatPlugin);
            LOGGER.info("Successfully registered voicechat discord plugin");
        } else {
            LOGGER.error("Failed to register voicechat discord plugin");
        }

        enable();

        Bukkit.getPluginManager().registerEvents(this, this);

        getServer().getCommandMap().register(getName(), new DvcBrigadierCommand());
        try {
            getCraftServer().getMethod("syncCommands").invoke(getServer());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        disable();

        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);
            LOGGER.info("Successfully unregistered voicechat discord plugin");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes", "UnstableApiUsage"})
    @EventHandler
    public void onCommandRegistered(final CommandRegisteredEvent<BukkitBrigadierCommandSource> event) {
        if (!(event.getCommand() instanceof DvcBrigadierCommand))
            return;

        platform.debug("registering pluginBrigadierCommand: " + event.getCommandLabel());
        final LiteralArgumentBuilder<CommandSourceStack> node = SubCommands.build(LiteralArgumentBuilder.literal(event.getCommandLabel()));
        event.setLiteral((LiteralCommandNode) node.build());
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {
        onPlayerJoin(e.getPlayer());
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent e) {
        onPlayerLeave(e.getPlayer().getUniqueId());
    }
}
