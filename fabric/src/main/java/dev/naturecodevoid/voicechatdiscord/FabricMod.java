package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.naturecodevoid.voicechatdiscord.Common.*;


public class FabricMod implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(PLUGIN_ID);

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void onInitializeServer() {
        if (platform == null)
            platform = new FabricPlatform();

        enable();

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            for (Commands.Command command : commands) {
                LiteralArgumentBuilder literal = LiteralArgumentBuilder.literal(command.name());
                command.builder().accept(literal);
                dispatcher.register(literal);
            }
        }));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> afterPlayerRespawn(api.fromServerPlayer(newPlayer)));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onPlayerLeave(handler.player.getUuid()));

        ServerLifecycleEvents.SERVER_STOPPING.register((server -> disable()));
    }
}
