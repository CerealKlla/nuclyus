package com.github.cerealklla.nuclyus.statbar.client;

import java.util.ArrayList;
import java.util.List;

import com.github.cerealklla.nuclyus.NuclyusMod;
import com.github.cerealklla.nuclyus.statbar.StatBarOwner;
import com.github.cerealklla.nuclyus.statbar.StatBarProvider;
import com.github.cerealklla.nuclyus.statbar.StatBarRegistry;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/**
 * Floating stat bars for any {@link StatBarOwner} (explicit user request, 2026-10-21: "create
 * custom health and mana indicators to float over the NPCs, the nametag style isn't sufficient,
 * and I want to be able to have the guards have names in the future. All entities should be
 * capable of having these health/mana representations").
 *
 * <p>Reuses vanilla's own nametag submission pipeline ({@code SubmitNodeCollector#submitNameTag}) --
 * confirmed against the real decompiled source that this already does exactly what a floating
 * world-space billboard needs (camera-facing text at an attachment point, correct light/distance
 * handling), the same primitive vanilla itself uses for a name/scoreboard-ranking pair stacked
 * above an entity's head. Drawn as colored block-character bars (same visual technique {@code
 * settlemynts.npc.NpcHealthBar} used), just submitted as an independent line through this handler
 * instead of being written into {@code Entity#setCustomName} -- so a real name can be set there in
 * the future with zero conflict. First-pass visual, functional before polished, same as this
 * suite's other HUD elements' own history (e.g. {@code lyfe.mana.client.ManaOverlay}'s plain
 * squares before real droplet icons) -- a real icon/quad-based bar is a natural future follow-up,
 * not attempted here.
 *
 * <p>Two-phase, since {@link RenderLivingEvent.Post} only ever gets a render-state snapshot, never
 * the live entity (confirmed against the decompiled source -- this version's renderer pipeline
 * extracts an immutable {@code LivingEntityRenderState} once per frame and submits from that, not
 * from the entity directly): {@link #onCanRenderNameTag} fires during state *extraction*, with the
 * live entity in hand, and stashes a plain snapshot onto the render state's own generic extension
 * slot ({@code EntityRenderState#setRenderData}, a real NeoForge-added "attach arbitrary data to a
 * render state" mechanism -- confirmed via {@code BaseRenderState}'s own source, not guessed);
 * {@link #onRenderLivingPost} reads that snapshot back at the actual draw moment.
 */
@EventBusSubscriber(modid = NuclyusMod.MODID, value = Dist.CLIENT)
public final class StatBarClientHandler {

    private static final ContextKey<StatBarSnapshot> SNAPSHOT_KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(NuclyusMod.MODID, "stat_bar_snapshot"));

    private static final int SEGMENTS = 10;
    private static final char FILLED = '█';
    private static final char EMPTY = '░';
    // One text line's worth of vertical lift in nametag-local space -- the exact same constant
    // vanilla's own submitNameDisplay uses to stack its scoreText/nameTag pair (9px line height *
    // 1.15 line spacing * the 0.025 nametag render scale).
    private static final float LINE_HEIGHT = 9.0F * 1.15F * 0.025F;

    // submitNameTag's own CameraRenderState parameter is only ever read for one field --
    // NameTagFeatureRenderer.Storage#add calls poseStack.mulPose(camera.orientation) and nothing
    // else (confirmed against the decompiled source) -- so a reused instance with just that one
    // field kept live is sufficient; RenderLivingEvent (unlike RenderNameTagEvent.DoRender) never
    // hands us a real CameraRenderState of our own to pass through.
    private static final CameraRenderState CAMERA_STATE = new CameraRenderState();

    private StatBarClientHandler() {
    }

    @SubscribeEvent
    static void onCanRenderNameTag(RenderNameTagEvent.CanRender event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !(living instanceof StatBarOwner)) {
            return;
        }
        List<StatBarSnapshot.Bar> bars = new ArrayList<>();
        for (StatBarProvider provider : StatBarRegistry.providers()) {
            if (provider.isApplicable(living)) {
                bars.add(new StatBarSnapshot.Bar(provider.current(living), provider.max(living), provider.color()));
            }
        }
        event.getEntityRenderState().setRenderData(SNAPSHOT_KEY, bars.isEmpty() ? null : new StatBarSnapshot(bars));
    }

    @SubscribeEvent
    static void onRenderLivingPost(RenderLivingEvent.Post<?, ?, ?> event) {
        StatBarSnapshot snapshot = event.getRenderState().getRenderData(SNAPSHOT_KEY);
        if (snapshot == null || snapshot.bars().isEmpty()) {
            return;
        }
        CAMERA_STATE.orientation.set(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        PoseStack poseStack = event.getPoseStack();
        var state = event.getRenderState();
        poseStack.pushPose();
        // Starts one line above wherever the real name tag/score text would sit, so a future real
        // name never collides with these bars -- same stacking technique vanilla's own
        // submitNameDisplay uses (translate, then submit, repeated), just starting past it instead
        // of before it.
        poseStack.translate(0.0F, LINE_HEIGHT, 0.0F);
        for (StatBarSnapshot.Bar bar : snapshot.bars()) {
            event.getSubmitNodeCollector().submitNameTag(
                    poseStack, state.nameTagAttachment, 0, barText(bar), !state.isDiscrete,
                    state.lightCoords, state.distanceToCameraSq, CAMERA_STATE);
            poseStack.translate(0.0F, LINE_HEIGHT, 0.0F);
        }
        poseStack.popPose();
    }

    private static Component barText(StatBarSnapshot.Bar bar) {
        float max = bar.max();
        float fraction = max > 0.0F ? Math.max(0.0F, Math.min(1.0F, bar.current() / max)) : 0.0F;
        int filled = Math.round(fraction * SEGMENTS);
        StringBuilder text = new StringBuilder(SEGMENTS);
        for (int i = 0; i < SEGMENTS; i++) {
            text.append(i < filled ? FILLED : EMPTY);
        }
        MutableComponent component = Component.literal(text.toString());
        return component.withStyle(style -> style.withColor(bar.color()));
    }
}
