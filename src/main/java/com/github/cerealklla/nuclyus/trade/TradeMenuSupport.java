package com.github.cerealklla.nuclyus.trade;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Shared "don't drop/lose anything on screen close" safeguard for a four-lane trading menu -- same
 * concern as {@code Kyt.loadout.LoadoutMenu#removed}'s own precedent (never drop a carried item on
 * the ground, never discard a real one), generalized so every trading menu in the suite doesn't
 * need to hand-roll it. Call this from the menu's own {@code removed(Player)} override, INSTEAD of
 * calling {@code super.removed(player)} (which is the one vanilla method actually responsible for
 * dropping a carried-but-unplaced item on the ground).
 */
public final class TradeMenuSupport {

    private TradeMenuSupport() {
    }

    /**
     * Resolves the menu's carried (cursor) stack: discarded if conjured (never a real possession),
     * returned to the player's own inventory (or dropped at their feet if it's full -- never onto
     * the ground beyond that, matching every other "give it back" fallback in this suite) if real.
     */
    public static void resolveCarried(AbstractContainerMenu menu, Player player) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        menu.setCarried(ItemStack.EMPTY);
        if (!ConjuredItems.isConjured(carried)) {
            giveOrPlaceBack(player, carried);
        }
    }

    /**
     * Returns every real (non-conjured) stack currently sitting in {@code container} to the player's
     * own inventory -- for a sell-staging container, whose contents are real items the player hasn't
     * been paid for yet, so they must never simply vanish if the screen closes before Confirm. A
     * conjured-only container (e.g. a cart's "Buy" staging area) needs no such pass -- its contents
     * were never real to begin with, and are safe to just let go out of scope.
     */
    public static void returnRealContents(Container container, Player player) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && !ConjuredItems.isConjured(stack)) {
                giveOrPlaceBack(player, stack);
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private static void giveOrPlaceBack(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }
}
