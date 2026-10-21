package com.github.cerealklla.nuclyus.statbar;

import net.minecraft.world.entity.LivingEntity;

/** Built-in Health bar -- registered unconditionally in {@code NuclyusMod#commonSetup}, since every {@code LivingEntity} already has real vanilla health with no dependency needed. Only applicable while not already at full health, matching the convention {@code settlemynts.npc.NpcHealthBar} established. */
public final class HealthStatBarProvider implements StatBarProvider {

    private static final int COLOR = 0xCC0000;

    @Override
    public boolean isApplicable(LivingEntity entity) {
        float max = entity.getMaxHealth();
        return max > 0.0F && entity.getHealth() < max;
    }

    @Override
    public float current(LivingEntity entity) {
        return entity.getHealth();
    }

    @Override
    public float max(LivingEntity entity) {
        return entity.getMaxHealth();
    }

    @Override
    public int color() {
        return COLOR;
    }
}
