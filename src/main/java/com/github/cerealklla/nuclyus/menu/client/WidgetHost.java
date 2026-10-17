package com.github.cerealklla.nuclyus.menu.client;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

/**
 * Implemented by any {@code Screen} that can host a {@link NuclyusMenu}'s real widgets (Quest
 * Log's action buttons, etc.) -- {@code Screen#addRenderableWidget}/{@code #removeWidget} are both
 * {@code protected}, so a tab (a plain object, not a {@code Screen} subclass) can't call them
 * directly on whatever {@code host} it's handed via {@link NuclyusMenu#attachHost}, even though
 * it's the right instance. {@code NuclyusMenuScreen} implements this; so does any mod's own
 * standalone screen that reuses the same tab class for a non-Nuclyus keybind (e.g. Myssions'
 * {@code client.QuestLogScreen} for its standalone "J" path) -- a tab checks {@code host
 * instanceof WidgetHost} rather than assuming a specific concrete host class.
 */
public interface WidgetHost {
    <T extends GuiEventListener & Renderable & NarratableEntry> T addMenuWidget(T widget);

    void removeMenuWidget(GuiEventListener widget);
}
