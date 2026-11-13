package com.github.cerealklla.nuclyus.portal;

import com.github.cerealklla.nuclyus.config.NuclyusConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Portal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The other half of the Nether/End portal rework, alongside {@link VanillaPortalAutoTeleportListener}
 * -- with vanilla's own automatic crossing disabled, a player right-clicking a Nether/End portal
 * block now triggers it manually via {@link PortalCrossing#attempt}. Added 2026-11-13.
 *
 * <p>Fires on both logical sides (see {@code PlayerInteractEvent.RightClickBlock}'s own doc) but
 * only ever acts on the server, and only once per click ({@link InteractionHand#MAIN_HAND} only --
 * vanilla fires this event once per hand, and a single click should only ever attempt one crossing).
 *
 * <p>If the player is riding something, the <em>vehicle</em> is the entity that actually crosses --
 * matching vanilla's own rule (a passenger's own {@code canUsePortal(false)} is always {@code false},
 * see {@code Entity#canUsePortal}) and, crucially, the behavior this is replacing: before this
 * feature, a mounted player crossed only because their mount's own {@code entityInside} fired and
 * its {@code teleportCrossDimension} recursively carried its rider along. Triggering the crossing on
 * the player directly instead would call {@code Entity#teleport} on a passenger, which dismounts it
 * first -- leaving the mount behind in the old dimension.
 */
public final class PlayerPortalRightClickListener {

    @SubscribeEvent
    public void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (!NuclyusConfig.DISABLE_VANILLA_PORTAL_AUTO_TELEPORT.get() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof Portal)) {
            return;
        }

        Entity crossingEntity = player.getRootVehicle();
        if (PortalCrossing.attempt(level, crossingEntity, pos)) {
            event.setCanceled(true);
        }
    }
}
