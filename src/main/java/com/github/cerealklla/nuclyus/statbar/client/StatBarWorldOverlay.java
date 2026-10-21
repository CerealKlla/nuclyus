package com.github.cerealklla.nuclyus.statbar.client;

import java.util.ArrayList;
import java.util.List;

import com.github.cerealklla.nuclyus.statbar.StatBarOwner;
import com.github.cerealklla.nuclyus.statbar.StatBarProvider;
import com.github.cerealklla.nuclyus.statbar.StatBarRegistry;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
            if (!(entity instanceof LivingEntity living) || !(living instanceof StatBarOwner)) {
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
            if (applicable.isEmpty()) {
                continue;
            }

            Vec3 worldPos = living.getPosition(partialTick).add(0.0, living.getBbHeight() + 0.3, 0.0);
            Vec3 relative = worldPos.subtract(cameraPos);
            Vector4f clip = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1.0F);
            viewProjection.transform(clip);
            if (clip.w <= 0.01F) {
                continue; // Behind the camera.
            }
            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;
            if (ndcX < -1.2F || ndcX > 1.2F || ndcY < -1.2F || ndcY > 1.2F) {
                continue; // Well outside the screen.
            }
            int screenX = Math.round((ndcX * 0.5F + 0.5F) * guiWidth);
            int screenY = Math.round((1.0F - (ndcY * 0.5F + 0.5F)) * guiHeight);

            int barTop = screenY;
            for (StatBarProvider provider : applicable) {
                drawBar(guiGraphics, screenX, barTop, provider.current(living), provider.max(living), provider.color());
                barTop += BAR_HEIGHT + BAR_SPACING;
            }
        }
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
