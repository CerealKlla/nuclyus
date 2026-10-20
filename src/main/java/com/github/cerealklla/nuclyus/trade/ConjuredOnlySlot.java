package com.github.cerealklla.nuclyus.trade;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A staging slot (e.g. a shop cart's "Buy" column) that only ever accepts {@link
 * ConjuredItems#isConjured conjured} stacks -- the real half of the four-lane pattern's enforcement
 * (the other half is {@link RealOnlySlot}): a conjured item can move freely between a {@link
 * CatalogSlot} and a slot like this one, but can never reach a real-item slot (a player's own
 * inventory, or a {@code RealOnlySlot}-backed sell staging area) directly, closing the "place a
 * never-paid-for item straight into my inventory" exploit at the slot level rather than by
 * hand-intercepting every click path.
 */
public class ConjuredOnlySlot extends Slot {

    public ConjuredOnlySlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return ConjuredItems.isConjured(stack);
    }
}
