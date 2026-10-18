package com.github.cerealklla.nuclyus.ping.client;

import java.util.ArrayList;
import java.util.List;

import com.github.cerealklla.nuclyus.ping.PingCandidatesPayload;
import com.github.cerealklla.nuclyus.ping.PingConstants;
import com.github.cerealklla.nuclyus.ping.Pingable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/**
 * Client-side half of the Pingable pipeline (design doc "Player-centric Pinging," 2026-10-18) --
 * finds every nearby {@link Pingable} by walking structures the client ALREADY tracks for
 * rendering (nearby loaded chunks' own block-entity maps, via the same {@code
 * LevelChunk#getBlockEntities()} approach {@code zone.PlotCraftingStructures} already uses
 * server-side; and the level's own loaded entities via {@code getEntitiesOfClass}, the same method
 * {@code plotsign.client.ClientProximityScanner} already proved out client-side) and a plain
 * {@code instanceof Pingable} check -- no registry anywhere, so any mod's own Pingable block/entity
 * is found automatically, purely through polymorphism. Only the resulting small candidate list
 * (positions/ids, not any real state) crosses the network, which is the actual point: "90% of the
 * goal is moving the cost off the server" -- the per-tick "what's nearby" scan now runs once per
 * player on that player's own machine instead of once per online player on the shared server.
 *
 * <p>Sends on every scan tick while something is actually nearby (so a {@link Pingable}'s own
 * cooldown logic keeps getting a fresh dispatch opportunity to decide whether to actually act), plus
 * exactly one more send the moment the candidate set becomes empty (so server-side state tied to
 * "nothing's pinging this anymore" isn't left waiting on a message that will never come) -- then
 * stays silent. Mirrors {@code ClientProximityScanner}'s own "don't transmit while nothing's
 * happening" shape, adapted since here the dispatch frequency itself (not just whether the set
 * changed) is part of {@link Pingable}'s own contract.
 */
public final class ClientPingScanner {

    private static int tickCounter;
    private static boolean lastScanHadCandidates;

    private ClientPingScanner() {
    }

    public static void tick() {
        tickCounter++;
        if (tickCounter % PingConstants.SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || mc.getConnection() == null) {
            return;
        }

        int radius = PingConstants.RADIUS_BLOCKS;
        double radiusSq = (double) radius * radius;
        BlockPos center = player.blockPosition();

        List<BlockPos> blockHits = new ArrayList<>();
        int minChunkX = (center.getX() - radius) >> 4, maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4, maxChunkZ = (center.getZ() + radius) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    if (!(entry.getValue() instanceof Pingable)) {
                        continue;
                    }
                    BlockPos pos = entry.getKey();
                    if (pos.distSqr(center) <= radiusSq) {
                        blockHits.add(pos.immutable());
                    }
                }
            }
        }

        List<Integer> entityHits = new ArrayList<>();
        AABB searchBox = new AABB(center).inflate(radius);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, searchBox, e -> e instanceof Pingable)) {
            entityHits.add(entity.getId());
        }

        boolean hasCandidates = !blockHits.isEmpty() || !entityHits.isEmpty();
        if (!hasCandidates && !lastScanHadCandidates) {
            return; // nothing nearby, nothing changed -- the overwhelmingly common case, costs nothing beyond the scan itself
        }
        lastScanHadCandidates = hasCandidates;
        mc.getConnection().send(new ServerboundCustomPayloadPacket(new PingCandidatesPayload(blockHits, entityHits)));
    }
}
