package com.github.cerealklla.nuclyus.statbar;

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
 */
public interface StatBarOwner {
}
