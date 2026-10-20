package com.github.cerealklla.nuclyus;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Plain "what's this item actually called" resolution from a bare {@link Identifier} -- extracted
 * 2026-10-20 after noticing the same {@code new ItemStack(item).getHoverName().getString()} (or the
 * registry-lookup-first variant of it) independently hand-written at 15+ call sites across Lyfe,
 * Settlemynts, and Myssions, each its own private helper. Side-agnostic (both {@code
 * BuiltInRegistries.ITEM} and {@code ItemStack#getHoverName} work identically on server and client),
 * so this lives in the plain root package rather than {@code .client}/{@code .server}. Only the one
 * call site that prompted the extraction (Lyfe's {@code craft.CraftingStructureMenu}) has been
 * migrated so far -- the other existing call sites are untouched, left for a future pass rather than
 * a broad unprompted refactor.
 */
public final class ItemDisplayNames {

    private ItemDisplayNames() {
    }

    /** The item's real display name, or {@code itemId}'s own string form if it isn't a registered item. */
    public static String of(Identifier itemId) {
        return BuiltInRegistries.ITEM.getOptional(itemId)
                .map(item -> new ItemStack(item).getHoverName().getString())
                .orElse(itemId.toString());
    }
}
