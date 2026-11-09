package com.github.cerealklla.nuclyus.statbar.client;

import java.util.ArrayList;
import java.util.List;

import com.github.cerealklla.nuclyus.statbar.StatBarOwner;
import com.github.cerealklla.nuclyus.statbar.StatBarProvider;
import com.github.cerealklla.nuclyus.statbar.StatBarRegistry;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Real floating Health/Mana bars, drawn as actual colored rectangles projected to screen space --
 * explicit user request, 2026-10-21, after rejecting the nametag-text version outright ("This
 * looks terrible... I already made it clear that the nametag approach is no longer viable"),
 * wanting the familiar vanilla-style bar look instead (reference screenshot: thin green bars over
 * cows).
 *
 * <p><b>Why this replaces the {@code RenderNameTagEvent}-based approach entirely</b>, not just its
 * visual: that approach needed a live {@code LivingEntity} (only available from
 * {@code RenderNameTagEvent.CanRender}, during render-state *extraction*) and a drawing surface
 * (only available from render-*submission* events, which never expose the entity at all) to meet
 * in the same place. Confirmed live via diagnostic logging that extraction's and submission's
 * render-state objects don't share identity for these entities' renderer chain
 * ({@code HumanoidMobRenderer}/{@code VillagerRenderer}) -- data stashed in one was never visible
 * to the other. A real graphical bar (not vanilla's own nametag text primitive) needs genuine world
 * -space geometry anyway, which only submission events can draw, so the same mismatch would have
 * blocked this approach too.
 *
 * <p>Sidesteps the whole problem: a {@link GuiLayer} (the same proven 2D-HUD mechanism this suite
 * already uses successfully -- {@code lyfe.mana.client.ManaOverlay}, {@code
 * lyfe.durability.client.EquipmentStatusOverlay}) has direct access to the client's own loaded
 * entities (no event-provided entity needed at all) and a real {@link GuiGraphicsExtractor} to
 * draw with, in the very same method call. Each {@link StatBarOwner} within range is projected from
 * its own world position to screen space using the camera's real view/projection matrices
 * ({@link Camera#getViewRotationProjectionMatrix}, confirmed against the decompiled source as the
 * exact precomputed view*projection matrix vanilla's own camera already maintains -- no need to
 * multiply the two separately), then drawn as a plain {@code GuiGraphicsExtractor#fill} rectangle
 * pair (background + proportional foreground) -- first-pass visual, functional before polished,
 * same precedent as this suite's other HUD elements.
 *
 * <p><b>{@link StatBarOwner#displayName()} added 2026-11-05</b> -- drawn as plain centered text
 * directly above the stacked bars, same projection/occlusion math already computed for the bars
 * themselves (no separate pass). An owner with a name but zero currently-applicable stat bars (e.g.
 * full health, no Mana) still shows -- the early-exit below now only skips an owner with neither.
 */
public final class StatBarWorldOverlay implements GuiLayer {

    private static final double MAX_DISTANCE_BLOCKS = 48.0;
    private static final double MAX_DISTANCE_SQ = MAX_DISTANCE_BLOCKS * MAX_DISTANCE_BLOCKS;
    private static final int BAR_WIDTH = 24;
    private static final int BAR_HEIGHT = 2;
    private static final int BAR_SPACING = 1;
    private static final int BACKGROUND_COLOR = 0xB0000000;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        Matrix4f viewProjection = camera.getViewRotationProjectionMatrix(new Matrix4f());
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        int guiWidth = guiGraphics.guiWidth();
        int guiHeight = guiGraphics.guiHeight();

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !(living instanceof StatBarOwner owner)) {
                continue;
            }
            if (living.distanceToSqr(minecraft.player) > MAX_DISTANCE_SQ) {
                continue;
            }
            List<StatBarProvider> applicable = new ArrayList<>();
            for (StatBarProvider provider : StatBarRegistry.providers()) {
                if (provider.isApplicable(living)) {
                    applicable.add(provider);
                }
            }
            Component name = owner.displayName();
            if (applicable.isEmpty() && name == null) {
                continue;
            }

            Vec3 worldPos = living.getPosition(partialTick).add(0.0, living.getBbHeight() + 0.3, 0.0);
            // Real follow-up, 2026-10-21: "is there any way to prevent it being visible through
            // terrain?" -- a screen-space overlay has no inherent depth test against the world at
            // all (that's the whole reason it sidesteps the render-state-identity problem), so
            // without this it draws through walls unconditionally. A plain block raycast from the
            // camera to a real point on the entity -- MISS means a clear line of sight.
            //
            // Real bug, found live 2026-11-07: "it's still removing name tags when I walk up to NPCs
            // which have 'G' menus" -- the raycast target used to be the elevated nameplate ANCHOR
            // point (bbHeight + 0.3 above the entity's feet), not a real point on the entity itself.
            // Standing close to an NPC inside a shop stall/plot structure (exactly the "G" menu case)
            // routinely has a counter, awning, or low ceiling directly over the NPC's own head -- the
            // entity is plainly visible, but that elevated anchor point pokes above/through the real
            // obstruction, occluding the whole bar+name even though nothing is actually hiding the
            // NPC. Raycasting to the entity's real eye position instead (the same point its own
            // rendered model is visibly NOT occluded at) fixes this -- the bar/name still DRAW at the
            // elevated anchor, only the visibility check moved.
            if (level.clip(new ClipContext(cameraPos, living.getEyePosition(partialTick), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player))
                    .getType() != HitResult.Type.MISS) {
                continue;
            }
            Vec3 relative = worldPos.subtract(cameraPos);
            Vector4f clip = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1.0F);
            viewProjection.transform(clip);
            if (clip.w <= 0.01F) {
                continue; // Behind the camera.
            }
            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;
            // Real bug, found live 2026-11-07: "as soon as I walk up right next to a named NPC their
            // name goes away" -- this used to skip drawing entirely once the projected point fell
            // outside a fixed +/-1.2 NDC margin. The anchor point sits at a FIXED world-space height
            // above the entity (bbHeight + 0.3), so at close range that same offset subtends a much
            // larger angle from the camera and gets pushed past the margin almost immediately --
            // vanishing exactly when standing right next to someone, not when genuinely far off to
            // the side. Clamping the final screen position (so it pins to the edge instead of being
            // skipped) is the correct fix -- a 2D HUD draw doesn't actually need the point to be
            // inside the 3D frustum at all, only "behind the camera" (clip.w above) is a real reason
            // to skip it outright.
            int screenX = Math.round((ndcX * 0.5F + 0.5F) * guiWidth);
            int screenY = Math.round((1.0F - (ndcY * 0.5F + 0.5F)) * guiHeight);
            screenX = Math.max(BAR_WIDTH, Math.min(guiWidth - BAR_WIDTH, screenX));
            screenY = Math.max(minecraft.font.lineHeight, Math.min(guiHeight - minecraft.font.lineHeight, screenY));

            int barTop = screenY;
            if (name != null) {
                barTop = drawName(guiGraphics, minecraft.font, screenX, barTop, name);
            }
            for (StatBarProvider provider : applicable) {
                drawBar(guiGraphics, screenX, barTop, provider.current(living), provider.max(living), provider.color());
                barTop += BAR_HEIGHT + BAR_SPACING;
            }
        }
    }

    /** @return the Y to resume drawing at, just below the name line. */
    private static int drawName(GuiGraphicsExtractor guiGraphics, Font font, int centerX, int top, Component name) {
        String text = name.getString();
        int width = font.width(text);
        guiGraphics.fill(centerX - width / 2 - 2, top, centerX + width / 2 + 2, top + font.lineHeight, BACKGROUND_COLOR);
        guiGraphics.text(font, text, centerX - width / 2, top + 1, 0xFFFFFFFF);
        return top + font.lineHeight + BAR_SPACING;
    }

    private static void drawBar(GuiGraphicsExtractor guiGraphics, int centerX, int top, float current, float max, int color) {
        int left = centerX - BAR_WIDTH / 2;
        guiGraphics.fill(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, BACKGROUND_COLOR);
        float fraction = max > 0.0F ? Math.max(0.0F, Math.min(1.0F, current / max)) : 0.0F;
        int filledWidth = Math.round(BAR_WIDTH * fraction);
        if (filledWidth > 0) {
            guiGraphics.fill(left, top, left + filledWidth, top + BAR_HEIGHT, 0xFF000000 | (color & 0xFFFFFF));
        }
    }
}
