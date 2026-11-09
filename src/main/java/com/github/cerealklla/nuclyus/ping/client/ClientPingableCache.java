package com.github.cerealklla.nuclyus.ping.client;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.github.cerealklla.nuclyus.NuclyusMod;
import com.github.cerealklla.nuclyus.ping.Pingable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Client-only, self-maintaining cache of every currently-loaded {@link Pingable} -- explicit user
 * design, 2026-11-06: "build a map and any pingable gets put up top to be scanned immediately
 * instead of searching each block every ping," the same proven self-registering-registry shape
 * {@code tinywyrlds.wyrldprint.interactive.TaggedInteractiveRegistry} already established for the
 * tagged lever system, just driven by real NeoForge lifecycle events instead of each implementor
 * calling in directly -- preserving {@link Pingable}'s own "zero registration needed anywhere"
 * property while still getting the performance win: {@link ClientPingScanner} now iterates this
 * small maintained set every scan tick instead of re-walking every loaded chunk's whole block-entity
 * map and running a fresh AABB entity query from scratch, each and every tick.
 *
 * <p>Updated purely by real load/unload events ("new chunk renders, update the pingable cache map"):
 * {@link ChunkEvent.Load}/{@link ChunkEvent.Unload} for block-entity Pingables, {@link
 * EntityJoinLevelEvent}/{@link EntityLeaveLevelEvent} for entity Pingables -- all four confirmed
 * (against the real NeoForge sources, not assumed) to fire on both logical sides, guarded here to
 * the client only. {@link #blockEntityPingables()}/{@link #entityPingables()} lazily drop anything
 * that's since been individually removed without a matching event (e.g. a single block entity
 * replaced without its whole chunk unloading -- there's no finer-grained NeoForge event for that one
 * specific case) -- self-healing on next use rather than needing a third event hook for a rare edge
 * case.
 */
@EventBusSubscriber(modid = NuclyusMod.MODID, value = Dist.CLIENT)
public final class ClientPingableCache {

    private static final Set<BlockEntity> BLOCK_ENTITY_PINGABLES = ConcurrentHashMap.newKeySet();
    private static final Set<Entity> ENTITY_PINGABLES = ConcurrentHashMap.newKeySet();

    private ClientPingableCache() {
    }

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        LevelChunk chunk = event.getChunk();
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof Pingable) {
                BLOCK_ENTITY_PINGABLES.add(blockEntity);
            }
        }
    }

    @SubscribeEvent
    static void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        BLOCK_ENTITY_PINGABLES.removeAll(event.getChunk().getBlockEntities().values());
    }

    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof Pingable) {
            ENTITY_PINGABLES.add(event.getEntity());
        }
    }

    @SubscribeEvent
    static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof Pingable) {
            ENTITY_PINGABLES.remove(event.getEntity());
        }
    }

    public static Collection<BlockEntity> blockEntityPingables() {
        BLOCK_ENTITY_PINGABLES.removeIf(BlockEntity::isRemoved);
        return BLOCK_ENTITY_PINGABLES;
    }

    public static Collection<Entity> entityPingables() {
        ENTITY_PINGABLES.removeIf(Entity::isRemoved);
        return ENTITY_PINGABLES;
    }
}
