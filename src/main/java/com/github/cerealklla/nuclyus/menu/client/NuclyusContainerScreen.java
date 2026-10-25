package com.github.cerealklla.nuclyus.menu.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * A real container screen (genuine {@link AbstractContainerMenu}, real drag-and-drop/shift-click
 * slots -- vanilla's own slot-click protocol handles all of that completely unmodified) that also
 * carries the same multi-tab chrome {@link NuclyusMenuScreen} uses, for the case where a tabbed
 * screen needs real inventory slots on (at most) one of its tabs. {@code NuclyusMenuScreen} itself
 * can't host that directly -- it's a plain {@code Screen} with no backing menu at all.
 *
 * <p><b>Real need this was built for, 2026-10-24</b>: Settlemynts' Plot Config Sign screen, whose
 * Shop tab used to pop its own separate {@code ShopCartMenu} window on top of/instead of the Plot
 * Config Sign's own tab bar -- explicit user correction: "Place it fully within the unified menu,
 * not as a separate area." {@link #tabs} here is the real tab list (Overview/Upgrade/Configure/
 * Shop/Garrison/Management, in that mod's case); the menu's own real {@code Slot}s (the Shop tab's
 * Buy/Sell/catalog/player-inventory lanes) simply live alongside them, each overriding {@code
 * Slot#isActive()} to report {@code false} whenever a *different* tab (or a different sub-tab, if
 * the owning tab has its own) is currently showing.
 *
 * <p><b>Why that's enough, with zero per-input-method guarding needed</b>: confirmed against the
 * real decompiled {@code AbstractContainerScreen} source -- every slot-touching entry point
 * ({@code extractSlots}/rendering, {@code getHoveredSlot}, and therefore every click/drag/release/
 * hotbar-swap path, all of which resolve the slot under the mouse through that one method first)
 * checks {@code slot.isActive()} before doing anything. An inactive slot draws nothing and can't be
 * hovered or clicked through any input path -- so a non-container tab's own widgets/custom input
 * (routed the same way {@link NuclyusMenuScreen} already routes them: tab's own {@link
 * NuclyusMenu#mouseClicked}/{@code mouseDragged}/{@code mouseScrolled}/{@code keyPressed} first,
 * then falling through to {@code super}, which is {@code AbstractContainerScreen}'s own widget-
 * dispatch-then-slot-click chain) never collides with a hidden slot sitting at the same screen
 * position. This is the one thing an earlier, single-tab version of this class said would need
 * "every input entry point individually guarded" to support -- it didn't; gating each {@code Slot}
 * itself was the actual answer.
 *
 * <p><b>Technique</b>: the tab bar occupies a fixed {@link #TAB_BAR_HEIGHT}-tall strip at the very
 * top of the window; {@link #init()} shifts {@code topPos} down by that amount right after {@code
 * super.init()} centers it -- every real slot position, label position, and hover/click hit-test in
 * {@code AbstractContainerScreen} is already computed relative to {@code leftPos}/{@code topPos}
 * (confirmed against the source: {@code extractContents} translates the whole render pass by
 * exactly those two values before drawing anything), so that one shift relocates the entire real
 * panel consistently with zero other changes.
 */
public abstract class NuclyusContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements WidgetHost {

    public static final int TAB_BAR_HEIGHT = 24;
    private static final int TAB_PADDING = 6;
    // Same palette as NuclyusMenuScreen's own tab bar -- kept as an independent copy rather than a
    // shared constant, since the two classes have no common supertype to hang one off and this is
    // small enough that duplicating it is lower-risk than refactoring the already-verified original.
    private static final int TAB_BAR_BG = 0xFF373737;
    private static final int TAB_ACTIVE_BG = 0xFFC6C6C6;
    private static final int TAB_INACTIVE_BG = 0xFF8B8B8B;
    private static final int TAB_DISABLED_BG = 0xFF4A4A4A;
    private static final int TAB_TEXT_COLOR = 0xFF202020;
    private static final int TAB_DISABLED_TEXT_COLOR = 0xFF909090;

    private final List<NuclyusMenu> tabs;
    private int activeIndex;

    protected NuclyusContainerScreen(T menu, Inventory inventory, Component title, List<NuclyusMenu> tabs, int imageWidth, int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);
        this.tabs = tabs;
        for (NuclyusMenu tab : tabs) {
            tab.attachHost(this);
        }
        this.activeIndex = firstEnabledIndex();
    }

    /** Convenience single-always-active-tab overload, for a container screen that draws its own content directly (no real {@link NuclyusMenu} page needed) and just wants the tab-bar label. */
    protected NuclyusContainerScreen(T menu, Inventory inventory, Component title, Component tabTitle, int imageWidth, int imageHeight) {
        this(menu, inventory, title, List.of(trivialTab(tabTitle)), imageWidth, imageHeight);
    }

    private static NuclyusMenu trivialTab(Component title) {
        return new NuclyusMenu() {
            @Override
            public Component title() {
                return title;
            }

            @Override
            public void onShown() {
            }

            @Override
            public void onHidden() {
            }

            @Override
            public void render(GuiGraphicsExtractor graphics, Rect2i area, int mouseX, int mouseY, float partialTick) {
            }
        };
    }

    private int firstEnabledIndex() {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).enabled()) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void init() {
        super.init();
        this.topPos += TAB_BAR_HEIGHT;
        this.topPos += extraTopOffset();
        if (!tabs.isEmpty()) {
            tabs.get(activeIndex).onShown();
        }
    }

    /**
     * Extra vertical shift applied to {@link #topPos} right after the tab bar's own {@link
     * #TAB_BAR_HEIGHT}, before any tab's {@link NuclyusMenu#onShown()} runs -- for a subclass whose
     * active tab needs more clearance than the generic tab bar alone provides (e.g. a tab with its
     * own second tab tier, which {@link #slotPanelTop()} would otherwise sit underneath). Must run
     * before {@code onShown()}, not after, since a tab that adds real widgets there (via {@link
     * WidgetHost#addMenuWidget}) positions them off {@link #slotPanelTop()}/{@link #slotPanelLeft()}
     * at that exact point -- added 2026-10-24 rather than having a subclass override {@link #init()}
     * itself and shift {@code topPos} after the fact, which would leave those first-shown widgets
     * stuck at the pre-shift position. Defaults to none.
     */
    protected int extraTopOffset() {
        return 0;
    }

    @Override
    public Rect2i contentArea() {
        return new Rect2i(0, TAB_BAR_HEIGHT, width, Math.max(0, height - TAB_BAR_HEIGHT));
    }

    /** The real slot panel's own screen position -- for a tab that needs to align its own decorations (headers, buttons) with real slots it doesn't directly own the layout of. */
    public int slotPanelLeft() {
        return leftPos;
    }

    /** @see #slotPanelLeft() */
    public int slotPanelTop() {
        return topPos;
    }

    @Override
    public <W extends GuiEventListener & Renderable & NarratableEntry> W addMenuWidget(W widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public void removeMenuWidget(GuiEventListener widget) {
        removeWidget(widget);
    }

    /** Same text-width-based sizing {@code NuclyusMenuScreen} uses -- see that class's own doc for the live-test bug this avoids. */
    private Rect2i[] tabRects() {
        Rect2i[] rects = new Rect2i[tabs.size()];
        if (tabs.isEmpty()) {
            return rects;
        }
        int[] widths = new int[tabs.size()];
        int sum = 0;
        for (int i = 0; i < tabs.size(); i++) {
            widths[i] = font.width(tabs.get(i).title()) + TAB_PADDING * 2;
            sum += widths[i];
        }
        int lastIndex = widths.length - 1;
        widths[lastIndex] = Math.max(widths[lastIndex], width - (sum - widths[lastIndex]));
        int x = 0;
        for (int i = 0; i < widths.length; i++) {
            rects[i] = new Rect2i(x, 0, widths[i], TAB_BAR_HEIGHT);
            x += widths[i];
        }
        return rects;
    }

    private void switchTo(int index) {
        if (index == activeIndex) {
            return;
        }
        tabs.get(activeIndex).onHidden();
        activeIndex = index;
        tabs.get(activeIndex).onShown();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!tabs.isEmpty()) {
            tabs.get(activeIndex).render(graphics, contentArea(), mouseX, mouseY, partialTick);
        }

        graphics.fill(0, 0, width, TAB_BAR_HEIGHT, TAB_BAR_BG);
        Rect2i[] rects = tabRects();
        for (int i = 0; i < tabs.size(); i++) {
            NuclyusMenu tab = tabs.get(i);
            Rect2i rect = rects[i];
            boolean enabled = tab.enabled();
            boolean active = i == activeIndex;
            int bg = !enabled ? TAB_DISABLED_BG : active ? TAB_ACTIVE_BG : TAB_INACTIVE_BG;
            graphics.fill(rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), bg);
            int textColor = enabled ? TAB_TEXT_COLOR : TAB_DISABLED_TEXT_COLOR;
            graphics.text(font, tab.title(),
                    rect.getX() + (rect.getWidth() - font.width(tab.title())) / 2,
                    rect.getY() + (TAB_BAR_HEIGHT - font.lineHeight) / 2,
                    textColor, false);
            if (!enabled && tab.disabledReason() != null && rect.contains(mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(font, tab.disabledReason(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.y() < TAB_BAR_HEIGHT) {
            Rect2i[] rects = tabRects();
            for (int i = 0; i < tabs.size(); i++) {
                if (rects[i].contains((int) event.x(), (int) event.y()) && tabs.get(i).enabled()) {
                    switchTo(i);
                    return true;
                }
            }
            return super.mouseClicked(event, doubleClick);
        }
        if (!tabs.isEmpty() && tabs.get(activeIndex).mouseClicked(contentArea(), event, doubleClick)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!tabs.isEmpty() && tabs.get(activeIndex).mouseDragged(contentArea(), event, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!tabs.isEmpty() && tabs.get(activeIndex).mouseScrolled(contentArea(), mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!tabs.isEmpty() && tabs.get(activeIndex).keyPressed(contentArea(), event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (!tabs.isEmpty()) {
            tabs.get(activeIndex).onHidden();
        }
        super.onClose();
    }
}
