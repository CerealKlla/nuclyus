package com.github.cerealklla.nuclyus.portal;

/**
 * A single-tick, re-entrant escape hatch from {@link VanillaPortalAutoTeleportListener}'s own
 * cancellation. Added alongside {@code NuclyusConfig#DISABLE_VANILLA_PORTAL_AUTO_TELEPORT},
 * 2026-11-13.
 *
 * <p>The listener cancels {@code EntityTravelToDimensionEvent} whenever the traveling entity is
 * currently standing in a real {@code NetherPortalBlock}/{@code EndPortalBlock} -- the only
 * reliable, generic signal available (confirmed against the decompiled vanilla source: {@code
 * PortalProcessor} keeps its own {@code Portal} reference {@code private} with no accessor, so
 * there's no way to ask "was this teleport call vanilla's own automatic one" directly). That
 * signal is necessarily also true for a deliberate, mod-initiated cross-dimension {@code
 * Entity#teleport(TeleportTransition)} call made while the entity physically overlaps a real
 * portal block -- e.g. Caryvan's wagon/team-mount being towed through the same portal plane a
 * lead mount just crossed, or this mod's own {@link PortalCrossing#attempt} recovering an entity
 * that was deliberately snapped onto the portal block first (Settlemynts'
 * {@code guardhouse.GuardReturnToDimensionGoal}). Both would be wrongly cancelled without this.
 *
 * <p>Any caller that is about to call {@code Entity#teleport(TeleportTransition)} (or anything
 * that results in one) for a reason OTHER than vanilla's own per-tick portal-standing detection
 * wraps that call in {@link #run(Runnable)} so the listener leaves it alone, regardless of
 * whether the entity happens to be standing on a real portal block at that exact moment.
 */
public final class PortalAutoTeleportBypass {

    private static boolean active;

    private PortalAutoTeleportBypass() {
    }

    public static boolean isActive() {
        return active;
    }

    /** Re-entrant -- a nested call (e.g. a passenger's own recursive teleport) stays bypassed too. */
    public static void run(Runnable action) {
        boolean previous = active;
        active = true;
        try {
            action.run();
        } finally {
            active = previous;
        }
    }
}
