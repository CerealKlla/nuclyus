package com.github.cerealklla.nuclyus.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;

/**
 * Manually performs the real vanilla Nether/End portal crossing for a given entity standing at a
 * given portal block -- the single reusable replacement for vanilla's own automatic per-tick
 * {@code Entity#handlePortal()} path, which {@link VanillaPortalAutoTeleportListener} disables.
 * Added 2026-11-13 alongside that listener and the new right-click-to-cross feature; see this
 * mod's own CLAUDE.md "Plans for future" entry for the full feature this is part of.
 *
 * <p>Deliberately reuses vanilla's own real logic rather than re-deriving it: {@link
 * Portal#getPortalDestination} is the exact same method {@code NetherPortalBlock}/{@code
 * EndPortalBlock} already implement to compute an exit (finding/creating the matching portal on
 * the other side, or the End's own spawn platform), and {@code Entity#teleport(TeleportTransition)}
 * is vanilla's one real cross-dimension execution path, already handling passengers recursively.
 * This mirrors {@code NetherPortalBlock}/{@code EndPortalBlock#entityInside}'s own body (the
 * {@code canUsePortal}/end-credits gating) plus {@code Entity#handlePortal}'s own gating ({@code
 * ServerLevel#isAllowedToEnterPortal}/{@code Entity#canTeleport}) exactly, just invoked once,
 * directly, instead of accumulated across ticks via {@code PortalProcessor}.
 */
public final class PortalCrossing {

    private PortalCrossing() {
    }

    /**
     * @param level      the entity's current level -- must be a real {@link ServerLevel}
     * @param entity     the entity to cross (the player themself, or their root vehicle if mounted --
     *                   the caller decides; matches vanilla's own "the vehicle's entityInside runs,
     *                   not the passenger's" rule, see {@code Entity#canUsePortal})
     * @param portalPos  the portal block the entity is at
     * @return {@code true} if a crossing (or, for an unseen End Portal, the end-credits sequence)
     *         was actually performed
     */
    public static boolean attempt(ServerLevel level, Entity entity, BlockPos portalPos) {
        if (!entity.canUsePortal(false)) {
            return false;
        }
        BlockState state = level.getBlockState(portalPos);
        if (!(state.getBlock() instanceof Portal portal)) {
            return false;
        }

        if (state.getBlock() instanceof EndPortalBlock
                && level.dimension() == Level.END
                && entity instanceof ServerPlayer player
                && !player.seenCredits) {
            player.showEndCredits();
            return true;
        }

        TeleportTransition transition = portal.getPortalDestination(level, entity, portalPos);
        if (transition == null) {
            return false;
        }
        ServerLevel newLevel = transition.newLevel();
        if (!level.isAllowedToEnterPortal(newLevel) || (newLevel.dimension() != level.dimension() && !entity.canTeleport(level, newLevel))) {
            return false;
        }

        PortalAutoTeleportBypass.run(() -> entity.teleport(transition));
        return true;
    }
}
