package dev.naturecodevoid.voicechatdiscord;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.naturecodevoid.voicechatdiscord.VoicechatDiscord.*;

public class FabricMod implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(PLUGIN_ID);

    @Override
    public void onInitializeServer() {
        if (platform == null)
            platform = new FabricPlatform();

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("startdiscordvoicechat").executes(context -> {
                runStartCommand(context.getSource().getPlayer());
                return 1;
            }));
        }));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            onPlayerLeave(handler.player.getUuid());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register((server -> {
            new Thread(VoicechatDiscord::disable).start();
        }));

        enable();
    }
}
