package com.github.cerealklla.nuclyus.menu.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

/**
 * The render/input contract every tab in the shared "K" menu implements -- see {@link
 * MenuContributor} for how a mod contributes one of these. Every input method defaults to "not
 * handled" so a tab only has to override what it actually needs (a read-only info tab like
 * Character Info needs none of them).
 */
public interface NuclyusMenu {

    Component title();

    /** False greys out this tab's button in the tab bar (e.g. a Map tab before its unlock level). */
    default boolean enabled() {
        return true;
    }

    /** Tooltip shown while hovering a disabled tab's button. Ignored while {@link #enabled()} is true. */
    default Component disabledReason() {
        return null;
    }

    /**
     * Explicit sort key for the tab bar, ascending, ties broken by registration order -- guarantees
     * the tab order is deterministic regardless of which mod's class happens to load first.
     */
    default int order() {
        return 0;
    }

    /**
     * Called once, right after this tab is constructed (before {@link #onShown()}), with the real
     * top-level {@link Screen} hosting the menu. Tabs that need to push a sub-screen and return
     * (a confirm dialog, a bug-report screen, etc.) store this instead of assuming they themselves
     * are the top-level screen.
     */
    default void attachHost(Screen host) {
    }

    /**
     * Called every time this tab becomes the active one (including the first time the menu opens),
     * AND every time the host screen re-runs its own {@code init()} while this tab is already active
     * (a window resize, or returning from a pushed sub-screen like a confirm dialog -- vanilla always
     * clears a screen's widget list on {@code init()}, even on a repeat call against the same
     * instance). A tab that adds real widgets via {@link #attachHost} must re-add them here every
     * time, not just once -- this method is the only signal it gets that they were just cleared.
     */
    void onShown();

    /** Called every time this tab stops being the active one, including when the whole menu closes. */
    void onHidden();

    /** Render this tab's content into {@code area} (below the tab bar). Mouse coordinates are screen-absolute, not area-relative. */
    void render(GuiGraphicsExtractor graphics, Rect2i area, int mouseX, int mouseY, float partialTick);

    default boolean mouseClicked(Rect2i area, MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    default boolean mouseDragged(Rect2i area, MouseButtonEvent event, double dragX, double dragY) {
        return false;
    }

    default boolean mouseScrolled(Rect2i area, double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    default boolean keyPressed(Rect2i area, KeyEvent event) {
        return false;
    }
}
