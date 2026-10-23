package com.github.cerealklla.nuclyus.menu.client;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.renderer.Rect2i;

/**
 * Implemented by any {@code Screen} that can host a {@link NuclyusMenu}'s real widgets (Quest
 * Log's action buttons, etc.) -- {@code Screen#addRenderableWidget}/{@code #removeWidget} are both
 * {@code protected}, so a tab (a plain object, not a {@code Screen} subclass) can't call them
 * directly on whatever {@code host} it's handed via {@link NuclyusMenu#attachHost}, even though
 * it's the right instance. {@code NuclyusMenuScreen} implements this; so does {@link
 * NuclyusContainerScreen} (added 2026-10-24, so the exact same tab classes work unchanged whether
 * hosted by a plain or a real-container screen); so does any mod's own standalone screen that
 * reuses the same tab class for a non-Nuclyus keybind (e.g. Myssions' {@code client.QuestLogScreen}
 * for its standalone "J" path) -- a tab checks {@code host instanceof WidgetHost} rather than
 * assuming a specific concrete host class.
 *
 * <p>{@link #contentArea()} joined this interface the same day {@link NuclyusContainerScreen} was
 * added -- every tab already called it on whatever concrete host type it assumed, so promoting it
 * here (instead of leaving it a `NuclyusMenuScreen`-only method) is what let every existing tab
 * class host-agnostic with a one-line cast change, no other logic touched.
 */
public interface WidgetHost {
    <T extends GuiEventListener & Renderable & NarratableEntry> T addMenuWidget(T widget);

    void removeMenuWidget(GuiEventListener widget);

    /** This host's content area (below its own tab bar, if any), in screen-absolute coordinates. */
    Rect2i contentArea();
}
