package com.github.cerealklla.nuclyus.worldgen.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * Lets a lone Dark Oak/Pale Oak sapling grow into its real (mega) tree on its own, instead of
 * silently never growing at all unless 3 more saplings are planted around it -- explicit user
 * report, 2026-11-16: "there are several types of trees that when their sapling is placed alone it
 * will not grow naturally, like Pale Oak, unless you plant multiple saplings around it. I want to
 * remove that restriction."
 *
 * <p><b>Confirmed against the real decompiled source before writing this</b> -- {@link
 * TreeGrower#growTree} first tries a 2x2-sapling-gated "mega tree" placement ({@code
 * isTwoByTwoSapling}, checked at all 4 possible corner offsets), and only on failure falls through
 * to a plain single-sapling {@code tree} feature. {@code TreeGrower.DARK_OAK}/{@code
 * TreeGrower.PALE_OAK} are the only two vanilla growers with a mega tree configured but
 * <b>no single-sapling fallback at all</b> ({@code tree} is {@code Optional.empty()} for both) --
 * every other mega-capable grower ({@code SPRUCE}, {@code JUNGLE}) already has one, which is
 * exactly why a lone Spruce/Jungle sapling already grows a normal-sized tree today and a lone Dark
 * Oak/Pale Oak sapling just never grows at all. That's the real, narrow bug this targets -- not "no
 * vanilla tree can ever ask for a 2x2," which would also wrongly disable Spruce/Jungle's existing,
 * working, and clearly intentional small-vs-mega variety.
 *
 * <p>Redirects the 2x2 check's very first candidate corner (the loop in {@code growTree} always
 * checks {@code (dx=0, dz=0)} -- the sapling's own position -- first) to report a match whenever
 * the grower has no single-sapling {@code tree} fallback, regardless of whether real neighboring
 * saplings exist. The surrounding vanilla code then clears/places exactly as if a real 2x2 had been
 * found, anchored at the lone sapling's own position -- so this grows the exact same authentic mega
 * tree shape (thick 2-wide trunk included), just without requiring 3 more saplings first. A grower
 * that DOES have a single-sapling fallback (Spruce, Jungle) is completely unaffected: the redirect
 * only overrides the "no real match found" case, so a real 2x2 for those still takes priority, and a
 * lone Spruce/Jungle sapling still falls through to its existing, correct single-tree chance exactly
 * as before.
 *
 * <p>No NeoForge event covers this (confirmed: no {@code SaplingGrowTreeEvent} or equivalent exists
 * in this NeoForge version) -- a Mixin on the real vanilla chokepoint is the only way to change
 * which tree variant a lone sapling resolves to.
 */
@Mixin(TreeGrower.class)
public abstract class TreeGrowerMixin {

    @Shadow
    @Final
    private Optional<ResourceKey<ConfiguredFeature<?, ?>>> tree;

    /** Shadows the real method so it stays callable below -- Mixin resolves this declaration back to {@code TreeGrower}'s own private static method at weave time; the body here is never actually executed. */
    @Shadow
    private static boolean isTwoByTwoSapling(BlockState state, BlockGetter level, BlockPos pos, int ox, int oz) {
        throw new AssertionError("Mixin shadow not applied");
    }

    @Redirect(
            method = "growTree",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/grower/TreeGrower;isTwoByTwoSapling(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;II)Z"
            )
    )
    private boolean nuclyus$allowSingleSaplingMegaGrowth(BlockState state, BlockGetter level, BlockPos pos, int ox, int oz) {
        if (isTwoByTwoSapling(state, level, pos, ox, oz)) {
            return true;
        }
        // Only the lone sapling's own position (the loop's first-checked corner) stands in for a
        // missing real 2x2 -- and only for a grower with no single-sapling fallback at all, see this
        // class's own doc.
        return ox == 0 && oz == 0 && this.tree.isEmpty();
    }
}
