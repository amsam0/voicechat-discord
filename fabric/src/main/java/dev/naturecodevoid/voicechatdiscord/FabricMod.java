package dev.naturecodevoid.voicechatdiscord;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static dev.naturecodevoid.voicechatdiscord.Constants.PLUGIN_ID;
import static dev.naturecodevoid.voicechatdiscord.Core.*;

public class FabricMod implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(PLUGIN_ID);

    @Override
    public void onInitializeServer() {
        if (platform == null)
            platform = new FabricPlatform();

        enable();

        ModContainer svcMod = FabricLoader.getInstance().getModContainer("voicechat").orElse(null);
        checkSVCVersion(svcMod != null ? svcMod.getMetadata().getVersion().toString() : null);

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> dispatcher.register(SubCommands.build(literal("dvc")))));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onPlayerJoin(handler.player));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onPlayerLeave(handler.player.getUuid()));

        ServerLifecycleEvents.SERVER_STOPPING.register((server -> disable()));
    }
}
