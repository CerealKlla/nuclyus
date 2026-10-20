package com.github.cerealklla.nuclyus.trade;

import com.github.cerealklla.nuclyus.NuclyusMod;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration point for the generic trading-menu data components -- extracted into Nuclyus
 * 2026-10-20 ("we should probably centralize our custom GUI code into Nuclyus"), built for
 * Settlemynts' shop cart redesign but deliberately generic (no Settlemynts-specific naming/logic) so
 * any future shop-like feature in this suite can reuse the same four-lane pattern.
 */
public final class ModTradeComponents {

    private ModTradeComponents() {
    }

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NuclyusMod.MODID);

    /**
     * Marks an {@link net.minecraft.world.item.ItemStack} as "conjured" -- not a real possession,
     * only ever a staged intent (e.g. a shop's catalog, or a pending cart line) that must never
     * reach a player's real inventory directly. See {@link ConjuredItems}' own doc for the
     * enforcement this backs. Deliberately NOT {@code networkSynchronized} -- the only thing that
     * ever reads this is server-side {@code mayPlace}/{@code mayPickup} lane enforcement; a
     * conjured stack only ever lives transiently inside a trading menu's own non-player-facing
     * containers (never saved to a real world/player inventory), so the client never needs to know.
     * {@code persistent} anyway, cheaply, as a defense-in-depth signal in case a conjured stack ever
     * ends up somewhere unexpected.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CONJURED =
            DATA_COMPONENTS.registerComponentType("conjured", builder -> builder.persistent(Codec.BOOL));
}
