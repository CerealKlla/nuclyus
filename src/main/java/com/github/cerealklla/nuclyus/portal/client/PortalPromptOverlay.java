package com.github.cerealklla.nuclyus.portal.client;

import com.github.cerealklla.nuclyus.config.NuclyusConfig;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * "Right Click to use Portal" -- shown above the hotbar/health row whenever the player is looking
 * at a real Nether/End portal block within normal interaction range, i.e. exactly when a right
 * click would actually trigger {@code PortalCrossing#attempt} via {@code
 * PlayerPortalRightClickListener}. Added 2026-11-13, explicit user request: once vanilla's own
 * automatic in-portal crossing is disabled, standing in a portal does nothing at all with no
 * feedback of any kind -- "so that it's not unclear what's happening."
 *
 * <p>Reads the client's own already-computed {@code Minecraft#hitResult} (the same raycast vanilla
 * itself uses for the crosshair/block-outline every frame) rather than running a second raycast of
 * its own -- if the player is currently targeting a portal block, a right click would hit it. This
 * naturally also means the hint only shows for a portal the player could plausibly act on (within
 * the client's own interaction/pick range), with zero extra per-frame cost beyond one
 * {@code instanceof} check pulled from state that's already computed regardless.
 *
 * <p>Gated on {@code NuclyusConfig#DISABLE_VANILLA_PORTAL_AUTO_TELEPORT} -- a {@code SERVER}-type
 * config, synced to the client on join, so this correctly reflects whatever the connected server
 * actually has the feature set to. If that's off, standing in a portal still works the old vanilla
 * way and no prompt is needed.
 */
public final class PortalPromptOverlay implements GuiLayer {

    private static final Component TEXT = Component.translatable("hud.nuclyus.right_click_portal");

    // Placeholder/tunable, eyeballed clearance above the hotbar + health/food/air row + XP bar --
    // same honest-first-guess spirit as every other overlay margin in this suite (e.g. xpbar.client
    // .XpBarOverlay's own BOTTOM_MARGIN), pending a real live-test look.
    private static final int BOTTOM_MARGIN = 80;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_COLOR = 0xA0000000;
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 2;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (!NuclyusConfig.DISABLE_VANILLA_PORTAL_AUTO_TELEPORT.get() || !isLookingAtPortal()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        String label = TEXT.getString();
        int textWidth = font.width(label);
        int centerX = guiGraphics.guiWidth() / 2;
        int bottom = guiGraphics.guiHeight() - BOTTOM_MARGIN;
        int textY = bottom - font.lineHeight;

        guiGraphics.fill(
                centerX - textWidth / 2 - PADDING_X, textY - PADDING_Y,
                centerX + textWidth / 2 + PADDING_X, bottom + PADDING_Y,
                BACKGROUND_COLOR);
        guiGraphics.text(font, label, centerX - textWidth / 2, textY, TEXT_COLOR);
    }

    private static boolean isLookingAtPortal() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            return false;
        }
        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHit) || hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        return player.level().getBlockState(blockHit.getBlockPos()).getBlock() instanceof Portal;
    }
}
