package com.github.cerealklla.nuclyus.trade;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The real-item half of the four-lane pattern -- see {@link ConjuredOnlySlot}'s own doc. Rejects any
 * {@link ConjuredItems#isConjured conjured} stack; a real player-inventory slot (or a sell-staging
 * slot like a shop cart's "Sell" column) should use this, so a conjured item can never land there
 * directly no matter which click/drag/shift-click path is used.
 */
public class RealOnlySlot extends Slot {

    public RealOnlySlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !ConjuredItems.isConjured(stack);
    }
}
