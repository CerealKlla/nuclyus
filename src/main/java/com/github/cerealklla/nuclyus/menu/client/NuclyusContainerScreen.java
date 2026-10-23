package com.github.cerealklla.nuclyus.menu.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * A real container screen (genuine {@link AbstractContainerMenu}, real drag-and-drop/shift-click
 * slots -- vanilla's own slot-click protocol handles all of that completely unmodified) that still
 * carries the same tab-bar chrome every non-container {@link NuclyusMenuScreen} host already uses,
 * for visual consistency across the suite (explicit user request, 2026-10-24: "move this into the
 * unified GUI," re: Settlemynts' NPC Shop screen). {@link NuclyusMenuScreen} itself can't host this
 * directly -- it's a plain {@code Screen} with no backing menu at all, and every existing tab there
 * (Skills, Quests, Mail, etc.) is a non-container, button/text-only page; a real container needs
 * vanilla's own {@code AbstractContainerScreen} machinery (slot click/drag/shift-click, carried-item
 * rendering, tooltips) to work at all, which only applies to a screen that genuinely extends it.
 *
 * <p><b>Deliberately a single always-active tab for now</b> -- the concrete need this was built for
 * (Settlemynts' Shop) only ever needs one. Supporting several tabs where exactly one is the live
 * container and others are plain {@link NuclyusMenu} pages would need every one of {@code
 * AbstractContainerScreen}'s input entry points (click/drag/release/key) individually guarded so a
 * click can't reach a real slot while a different tab is showing -- real, un-trivial surface this
 * class doesn't attempt until an actual second tab needs it (see the suite's own "don't design for
 * hypothetical future requirements" convention).
 *
 * <p><b>Technique</b>: the tab bar occupies a fixed {@link #TAB_BAR_HEIGHT}-tall strip at the very
 * top of the window; {@link #init()} shifts {@code topPos} down by that amount right after {@code
 * super.init()} centers it, and nothing else needs to change -- every real slot position, label
 * position, and hover/click hit-test in {@code AbstractContainerScreen} is already computed relative
 * to {@code leftPos}/{@code topPos} (confirmed against the real decompiled source: {@code
 * extractContents} translates the whole render pass by exactly those two values before drawing
 * anything), so the one shift relocates the entire real panel consistently with zero other changes.
 */
public abstract class NuclyusContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    public static final int TAB_BAR_HEIGHT = 24;
    private static final int TAB_PADDING = 6;
    // Same palette as NuclyusMenuScreen's own tab bar -- kept as an independent copy rather than a
    // shared constant, since the two classes have no common supertype to hang one off and this is
    // small enough that duplicating it is lower-risk than refactoring the already-verified original.
    private static final int TAB_BAR_BG = 0xFF373737;
    private static final int TAB_ACTIVE_BG = 0xFFC6C6C6;
    private static final int TAB_TEXT_COLOR = 0xFF202020;

    private final Component tabTitle;

    protected NuclyusContainerScreen(T menu, Inventory inventory, Component title, Component tabTitle, int imageWidth, int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);
        this.tabTitle = tabTitle;
    }

    @Override
    protected void init() {
        super.init();
        this.topPos += TAB_BAR_HEIGHT;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, TAB_BAR_HEIGHT, TAB_BAR_BG);
        int tabWidth = font.width(tabTitle) + TAB_PADDING * 2;
        graphics.fill(0, 0, tabWidth, TAB_BAR_HEIGHT, TAB_ACTIVE_BG);
        graphics.text(font, tabTitle,
                (tabWidth - font.width(tabTitle)) / 2,
                (TAB_BAR_HEIGHT - font.lineHeight) / 2,
                TAB_TEXT_COLOR, false);
    }
}
