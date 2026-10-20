package com.github.cerealklla.nuclyus.trade;

import net.minecraft.world.item.ItemStack;

/**
 * Plain helpers around {@link ModTradeComponents#CONJURED} -- see that field's own doc. "Conjured"
 * means not a real possession: a shop catalog's infinite display stack, or a cart line staged from
 * one, as opposed to a real item that actually came out of (or is going into) a player's own
 * inventory.
 */
public final class ConjuredItems {

    private ConjuredItems() {
    }

    /** A fresh copy of {@code template}, marked conjured. */
    public static ItemStack conjure(ItemStack template) {
        ItemStack copy = template.copy();
        copy.set(ModTradeComponents.CONJURED.get(), Boolean.TRUE);
        return copy;
    }

    public static boolean isConjured(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModTradeComponents.CONJURED.get()));
    }
}
