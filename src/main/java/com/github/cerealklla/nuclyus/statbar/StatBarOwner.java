package com.github.cerealklla.nuclyus.statbar;

import net.minecraft.network.chat.Component;

/**
 * Marker for a custom entity that wants floating stat bars (Health, Mana, ...) rendered above its
 * head via {@link StatBarRegistry}'s registered {@link StatBarProvider}s, instead of folded into
 * its real nametag the way {@code settlemynts.npc.NpcHealthBar} used to (explicit user request,
 * 2026-10-21: "the nametag style isn't sufficient, and I want to be able to have the guards have
 * names in the future") -- implementing this is the only thing an entity class needs to do; no
 * registration call anywhere, same "any mod's entity opts in directly" shape as {@code
 * ping.Pingable}. Deliberately opt-in rather than automatic for every {@code LivingEntity} -- the
 * underlying mechanism works for any entity ("all entities should be capable"), but showing a bar
 * over every vanilla zombie/cow by default would be noisy; a mod's own custom NPC classes choose to
 * implement this themselves.
 *
 * <p><b>{@link #displayName()}, added 2026-11-05</b> -- the "names in the future" this class' own
 * 2026-10-21 doc already anticipated. A real text line drawn by {@link
 * com.github.cerealklla.nuclyus.statbar.client.StatBarWorldOverlay} directly above this owner's
 * stacked stat bars (or alone, if it has none currently applicable) -- {@code null}, the default,
 * means no name line at all. An implementor resolves its own name however it owns that data (e.g.
 * Settlemynts' worker/guard entities read their own persisted {@code npc.NpcIdentity}); this
 * interface has no opinion on where a name comes from.
 */
public interface StatBarOwner {

    default Component displayName() {
        return null;
    }
}
