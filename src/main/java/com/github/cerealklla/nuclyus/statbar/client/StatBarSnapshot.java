package com.github.cerealklla.nuclyus.statbar.client;

import java.util.List;

/**
 * Stashed onto an {@code EntityRenderState} during {@code RenderNameTagEvent.CanRender} (the one
 * point in the frame with access to both the live entity and a render-state extension slot -- see
 * {@link StatBarClientHandler}'s own doc), then read back during {@code RenderLivingEvent.Post} to
 * actually draw. A plain snapshot, not a live reference -- the render state it's attached to may
 * outlive the tick the entity was read on.
 */
public record StatBarSnapshot(List<Bar> bars) {

    public record Bar(float current, float max, int color) {
    }
}
