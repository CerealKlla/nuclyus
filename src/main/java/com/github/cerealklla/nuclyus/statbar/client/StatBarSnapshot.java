package com.github.cerealklla.nuclyus.statbar.client;

import java.util.List;

import net.minecraft.world.phys.Vec3;

/**
 * Stashed onto an {@code EntityRenderState} during {@code RenderNameTagEvent.CanRender} (the one
 * point in the frame with access to both the live entity and a render-state extension slot -- see
 * {@link StatBarClientHandler}'s own doc), then read back during {@code RenderLivingEvent.Post} to
 * actually draw. A plain snapshot, not a live reference -- the render state it's attached to may
 * outlive the tick the entity was read on.
 *
 * <p>{@code attachment} is computed independently of {@code EntityRenderState#nameTagAttachment} --
 * real bug, 2026-10-21: that vanilla field is only ever populated when vanilla itself has *already*
 * decided to show the entity's real name ({@code shouldShowName}), which is false for every one of
 * these NPCs (none has a custom name set -- that's the whole point). {@code submitNameTag} silently
 * no-ops on a {@code null} attachment (confirmed against the real decompiled
 * {@code NameTagFeatureRenderer.Storage#add} -- its entire body is one {@code if (attachment !=
 * null)} with no else), so reusing that field meant nothing ever rendered for any of these NPCs no
 * matter their health/mana state.
 */
public record StatBarSnapshot(List<Bar> bars, Vec3 attachment) {

    public record Bar(float current, float max, int color) {
    }
}
