package com.github.cerealklla.nuclyus.trade;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * One row of an infinite "catalog" -- taking from it never depletes it. {@code container} holds one
 * already-{@link ConjuredItems#conjure conjured} template stack per slot (built by the caller, e.g.
 * from a shop's real listing data); this slot never accepts anything placed into it ({@link
 * #mayPlace} always false) and re-fills itself to the full template count the instant anything is
 * taken, so it always reads as "fully in stock." Taking from it always hands the player a
 * max-stack-sized conjured copy (not whatever partial amount a normal {@code Slot} would split
 * off), matching an infinite-supply catalog's own intuitive "click it, get a full stack" behavior.
 */
public class CatalogSlot extends Slot {

    public CatalogSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack template = getItem();
        return template.isEmpty() ? ItemStack.EMPTY : ConjuredItems.conjure(template.copyWithCount(template.getMaxStackSize()));
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        // Deliberately does NOT call super (which would decrement/clear the backing slot) -- the
        // template stack this slot's own Container holds is never actually touched, so it reads as
        // permanently "in stock" regardless of how many times it's taken from.
    }
}
