package com.github.cerealklla.nuclyus.portal;

import com.github.cerealklla.nuclyus.config.NuclyusConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/**
 * Cancels vanilla's automatic "stand in a Nether/End portal long enough and you cross" behavior,
 * gated on {@code NuclyusConfig#DISABLE_VANILLA_PORTAL_AUTO_TELEPORT}. Added 2026-11-13 -- see this
 * mod's own CLAUDE.md "Plans for future" entry: this was the root cause of the summon/mount
 * duplication exploit ({@code lyfe.summoning.SummonDimensionChangeListener}'s own doc) and guards
 * going permanently missing after chasing a target through a portal ({@code
 * settlemynts.guardhouse.GuardReturnToDimensionGoal}) -- both stemmed from an entity other than the
 * player independently wandering into a portal and crossing on its own, with nothing tracking it.
 * Players still cross deliberately via {@link PlayerPortalRightClickListener}; anything that still
 * needs a non-player entity to cross (that right-click's own vehicle-carrying, a Guard recovering
 * from being stranded) goes through {@link PortalCrossing#attempt} directly instead of vanilla's
 * automatic path.
 *
 * <p>{@code EntityTravelToDimensionEvent} fires for every {@code Entity#teleport(TeleportTransition)}
 * call, not just portal-driven ones (End Gateways, chorus fruit, a future mod's own cross-dimension
 * teleport) -- so this only cancels when the traveling entity is <em>currently standing in a real
 * {@link NetherPortalBlock}/{@link EndPortalBlock}</em>, the one signal that's both reliable and
 * correctly scoped (an End Gateway, or any other {@code Portal} implementor such as TinyWyrlds' own
 * entrance/exit blocks, never matches this check and is completely unaffected). See {@link
 * PortalAutoTeleportBypass}'s own doc for why a deliberate mod-initiated teleport that happens to
 * coincide with the entity standing on a real portal block (Caryvan's towed wagon/team mount,
 * {@link PortalCrossing}'s own recovery calls) must explicitly opt out of this check rather than
 * being distinguished from it automatically -- there's no reliable way to tell the two apart from
 * this event alone.
 */
public final class VanillaPortalAutoTeleportListener {

    @SubscribeEvent
    public void onTravelToDimension(final EntityTravelToDimensionEvent event) {
        if (!NuclyusConfig.DISABLE_VANILLA_PORTAL_AUTO_TELEPORT.get() || PortalAutoTeleportBypass.isActive()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof Level level)) {
            return;
        }
        if (isVanillaPortalBlock(level, entity.blockPosition()) || isVanillaPortalBlock(level, entity.blockPosition().above())) {
            event.setCanceled(true);
        }
    }

    private static boolean isVanillaPortalBlock(final Level level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof NetherPortalBlock || state.getBlock() instanceof EndPortalBlock;
    }
}
