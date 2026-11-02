package com.github.cerealklla.nuclyus.chunkload;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import com.github.cerealklla.nuclyus.NuclyusMod;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;

/**
 * Generic, multi-owner chunk force-load primitive -- design captured 2026-11-02 (see this mod's own
 * {@code context/design-document.md} Section 2 for the full rationale). Surfaced while designing
 * Settlemynts' Murmur Tower feature: that mod needed a second, independent reason to force-load a
 * settlement's chunks (keep awake whenever a player is within 300 blocks, not just at its existing
 * nightly midnight pass), and the two reasons couldn't safely share one settlement-keyed bookkeeping
 * map -- releasing one would silently yank chunks out from under the other. Explicit user direction:
 * generalize this into a shared primitive any mod can add independent claims to -- "various mods can
 * add entries into the list for chunks to keep awake... if there are other mods which still have a
 * claim to the chunk it stays awake until all claims are removed."
 *
 * <p>A single shared {@link TicketController} is correct and sufficient here -- NeoForge already
 * scopes {@code forceChunk}/ticket validation per-{@link ServerLevel} internally (confirmed via
 * Settlemynts' own pre-existing {@code zone.MidnightChunkWaker}, whose {@code validateTickets}
 * already fires once per loaded level). The real risk this class guards against is in its own
 * bookkeeping, not the underlying API: a packed {@link ChunkPos} coordinate (a plain {@code long})
 * is <b>not</b> globally unique across dimensions -- chunk (5,10) in the Overworld and chunk (5,10)
 * in the Nether pack to the identical {@code long}. Each claim's forced chunks are therefore tracked
 * per-dimension internally, and {@link #release} walks every dimension a given claim actually
 * touched rather than trusting a caller to pass the one correct {@link ServerLevel} back.
 */
public final class ChunkClaims {

    private static final TicketController CONTROLLER = new TicketController(
            Identifier.fromNamespaceAndPath(NuclyusMod.MODID, "chunk_claims"),
            ChunkClaims::validateTickets);

    private static final Map<Object, Map<ResourceKey<Level>, LongSet>> forcedChunksByClaim = new HashMap<>();

    private ChunkClaims() {
    }

    public static void registerController(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    /** No claim is ever expected to persist across a restart -- whatever logic wanted a chunk awake re-establishes its own claim on its own next check. */
    private static void validateTickets(ServerLevel level, TicketHelper helper) {
        for (UUID owner : helper.getEntityTickets().keySet()) {
            helper.removeAllTickets(owner);
        }
    }

    /**
     * {@link TicketController#forceChunk} only accepts a {@link BlockPos}/{@link net.minecraft.world.entity.Entity}/{@link UUID}
     * as its owner -- not an arbitrary claim key -- so each distinct {@code claimId} is deterministically
     * mapped to its own ticket-owner {@link UUID} (not simply {@code claimId}'s own {@code instanceId},
     * since two different claims for the same logical thing, e.g. a settlement's "midnight_wake" and
     * "proximity_wake" claims, legitimately share one {@code instanceId} and must still resolve to two
     * distinct ticket owners).
     */
    private static UUID ticketOwner(Object claimId) {
        return UUID.nameUUIDFromBytes(claimId.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Force-loads every chunk {@code positions} fall in, under {@code claimId}'s own ownership.
     * Safe to call again with more positions for the same claim (same or a different {@code level});
     * safe for the same chunk to be claimed by any number of <i>different</i> {@code claimId}s at
     * once -- it stays force-loaded as long as any one of them still references it.
     */
    public static void claim(ServerLevel level, Object claimId, Iterable<BlockPos> positions) {
        Map<ResourceKey<Level>, LongSet> byDimension = forcedChunksByClaim.computeIfAbsent(claimId, id -> new HashMap<>());
        LongSet chunks = byDimension.computeIfAbsent(level.dimension(), dim -> new LongOpenHashSet());
        UUID owner = ticketOwner(claimId);
        for (BlockPos pos : positions) {
            long packed = ChunkPos.pack(pos);
            if (chunks.add(packed)) {
                ChunkPos chunkPos = ChunkPos.unpack(packed);
                CONTROLLER.forceChunk(level, owner, chunkPos.x(), chunkPos.z(), true, false);
            }
        }
    }

    /**
     * Releases every chunk {@code claimId} itself added, in every dimension it ever claimed in --
     * deliberately no {@link ServerLevel} parameter, since a caller shouldn't have to remember which
     * dimension(s) a given claim spans. A chunk still referenced by a different claim remains
     * force-loaded -- this is the whole point, and falls out of each claim's own independent ticket
     * ownership, not a ref-count this method has to maintain by hand.
     */
    public static void release(MinecraftServer server, Object claimId) {
        Map<ResourceKey<Level>, LongSet> byDimension = forcedChunksByClaim.remove(claimId);
        if (byDimension == null) {
            return;
        }
        UUID owner = ticketOwner(claimId);
        for (Map.Entry<ResourceKey<Level>, LongSet> entry : byDimension.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue; // Dimension no longer loaded -- its tickets are already gone with it.
            }
            for (long packed : entry.getValue()) {
                ChunkPos chunkPos = ChunkPos.unpack(packed);
                CONTROLLER.forceChunk(level, owner, chunkPos.x(), chunkPos.z(), false, false);
            }
        }
    }
}
