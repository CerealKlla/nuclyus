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
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-side half of the Pingable pipeline (design doc "Player-centric Pinging," 2026-10-18) --
 * checks every currently-loaded {@link Pingable} (tracked by {@link ClientPingableCache}, kept
 * current purely by real chunk/entity load-unload events, not re-derived here) against its own
 * {@link Pingable#radiusBlocks}. Reworked 2026-11-06, explicit user design: the original version
 * re-walked every loaded chunk's whole block-entity map plus a fresh AABB entity query from scratch
 * on every single scan tick -- real, repeated, avoidable cost for something that only actually
 * changes when a chunk loads/unloads or an entity joins/leaves, "the same logic as the tag-able
 * levers" (see {@code tinywyrlds.wyrldprint.interactive.TaggedInteractiveRegistry}'s own
 * self-maintaining-map precedent). Only the resulting small candidate list (positions/ids, not any
 * real state) crosses the network, which is the actual point: "90% of the goal is moving the cost
 * off the server" -- the per-tick "what's nearby" check now runs once per player on that player's
 * own machine instead of once per online player on the shared server.
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

        List<BlockPos> blockHits = new ArrayList<>();
        for (BlockEntity blockEntity : ClientPingableCache.blockEntityPingables()) {
            Pingable pingable = (Pingable) blockEntity;
            double ownRadius = pingable.radiusBlocks();
            if (blockEntity.getBlockPos().distSqr(player.blockPosition()) <= ownRadius * ownRadius) {
                blockHits.add(blockEntity.getBlockPos());
            }
        }

        List<Integer> entityHits = new ArrayList<>();
        for (Entity entity : ClientPingableCache.entityPingables()) {
            Pingable pingable = (Pingable) entity;
            double ownRadius = pingable.radiusBlocks();
            if (entity.distanceToSqr(player) <= ownRadius * ownRadius) {
                entityHits.add(entity.getId());
            }
        }

        boolean hasCandidates = !blockHits.isEmpty() || !entityHits.isEmpty();
        if (!hasCandidates && !lastScanHadCandidates) {
            return; // nothing nearby, nothing changed -- the overwhelmingly common case, costs nothing beyond the scan itself
        }
        lastScanHadCandidates = hasCandidates;
        mc.getConnection().send(new ServerboundCustomPayloadPacket(new PingCandidatesPayload(blockHits, entityHits)));
    }
}
