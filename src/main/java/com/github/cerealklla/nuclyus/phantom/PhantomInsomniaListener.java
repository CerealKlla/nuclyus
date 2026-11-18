package com.github.cerealklla.nuclyus.phantom;

import com.github.cerealklla.nuclyus.config.NuclyusConfig;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * Real report, 2026-11-18: "I told you to disable the insomnia thing that forces phantoms to spawn
 * constantly. I guess that was a server setting that got reset with the wipe." {@code
 * spawn_phantoms} is a real gamerule ({@link GameRules#SPAWN_PHANTOMS}, consulted directly by
 * vanilla's {@code PhantomSpawner}), but a gamerule lives in the world save -- turning it off by hand
 * (e.g. via {@code /gamerule}) only lasts until the next world wipe resets it to vanilla's default
 * ({@code true}). Forcing it off on every boot, gated on {@link
 * NuclyusConfig#DISABLE_PHANTOM_INSOMNIA_SPAWNING}, makes the setting durable instead of a one-time
 * manual fix that has to be redone after every future wipe -- the exact kind of "prefer a standing
 * fix over repeating the same manual step" case this suite otherwise avoids, since the problem itself
 * (a wipe resetting world-save state) is recurring, not one-time.
 */
public final class PhantomInsomniaListener {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (!NuclyusConfig.DISABLE_PHANTOM_INSOMNIA_SPAWNING.get()) {
            return;
        }
        MinecraftServer server = event.getServer();
        server.getGameRules().set(GameRules.SPAWN_PHANTOMS, false, server);
    }
}
