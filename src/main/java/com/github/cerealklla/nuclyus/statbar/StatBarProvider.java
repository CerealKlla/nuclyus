package com.github.cerealklla.nuclyus.statbar;

import net.minecraft.world.entity.LivingEntity;

/**
 * One stat (Health, Mana, ...) a {@link StatBarOwner} can show floating above its head, registered
 * via {@link StatBarRegistry}. Nuclyus itself registers a built-in Health provider (any {@code
 * LivingEntity} already has real health, no dependency needed); a mod owning a different stat (e.g.
 * Lyfe's Mana) registers its own the same way, unconditionally, since Nuclyus is hard-required by
 * every mod in this suite.
 */
public interface StatBarProvider {

    /** Whether this stat should currently show for {@code entity} at all -- e.g. "not already full." */
    boolean isApplicable(LivingEntity entity);

    float current(LivingEntity entity);

    float max(LivingEntity entity);

    /** Packed RGB (0xRRGGBB) for this bar's filled segments. */
    int color();
}
