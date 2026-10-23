package com.github.cerealklla.nuclyus.menu.client;

import java.util.List;

import com.github.cerealklla.nuclyus.NuclyusModClient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

/**
 * The shared "K" menu's host screen -- owns the tab bar (top, fixed height) and delegates
 * render/click/drag/scroll/key input for the content area below to whichever {@link NuclyusMenu}
 * is active. Every tab across every registered {@link MenuContributor} is collected fresh in
 * {@link #init()} (same "rebuilt from scratch each open" convention every full-screen UI in this
 * suite already follows), sorted by {@link NuclyusMenu#order()}.
 */
public final class NuclyusMenuScreen extends Screen implements WidgetHost {

    private static final int TAB_BAR_HEIGHT = 24;
    private static final int TAB_PADDING = 6;
    private static final int TAB_BAR_BG = 0xFF373737;
    private static final int TAB_ACTIVE_BG = 0xFFC6C6C6;
    private static final int TAB_INACTIVE_BG = 0xFF8B8B8B;
    private static final int TAB_DISABLED_BG = 0xFF4A4A4A;
    private static final int TAB_TEXT_COLOR = 0xFF202020;
    private static final int TAB_DISABLED_TEXT_COLOR = 0xFF909090;
    private static final int BACKDROP_COLOR = 0xC0101010;

    private List<NuclyusMenu> tabs = List.of();
    private int activeIndex;
    private boolean initialized;

    public NuclyusMenuScreen() {
        super(Component.literal("Menu"));
    }

    /**
     * Fixed-tab variant (added 2026-10-23) for a feature-specific tabbed screen that wants this
     * host's tab-bar rendering/input delegation but isn't part of the shared cross-mod K menu
     * registry -- e.g. Settlemynts' Plot Config Sign, whose tab set is computed per-open from
     * server-sent flags, not collected from {@link NuclyusMenuRegistry}. Skips the registry
     * entirely; {@code fixedTabs} is used as-is (already in the caller's desired order -- no {@link
     * NuclyusMenu#order()} sort applied, since a fixed caller-built list has no registration-order
     * ambiguity to resolve).
     */
    public NuclyusMenuScreen(Component title, List<NuclyusMenu> fixedTabs) {
        super(title);
        this.tabs = fixedTabs;
        for (NuclyusMenu tab : tabs) {
            tab.attachHost(this);
        }
        this.activeIndex = firstEnabledIndex();
        this.initialized = true;
    }

    /**
     * Real gap found while porting Myssions' Quest Log (which pushes a {@code ConfirmScreen}/
     * {@code BugReportScreen} and restores by calling {@code setScreen} with the SAME host instance
     * passed via {@link NuclyusMenu#attachHost}, not a freshly-constructed one): vanilla calls a
     * screen's {@code init()} every time {@code setScreen} targets it, including this "restore the
     * same instance" case and plain window resizes -- not just on a genuinely new open. Rebuilding
     * {@link #tabs} from {@link NuclyusMenuRegistry#collectAllMenus()} on every such call would
     * silently discard every tab's live state (scroll position, selection, etc.) the instant a
     * pushed sub-screen like a confirm dialog closed. {@code tabs}/{@code activeIndex} are now only
     * ever built once per real open, gated on {@link #initialized}.
     */
    @Override
    protected void init() {
        if (!initialized) {
            tabs = NuclyusMenuRegistry.collectAllMenus();
            for (NuclyusMenu tab : tabs) {
                tab.attachHost(this);
            }
            activeIndex = firstEnabledIndex();
            initialized = true;
        }
        // Vanilla's own init() wrapper already cleared this screen's widget list before calling this
        // method, even on a repeat call -- re-running onShown() every time (not just the first) lets
        // a tab with real Button widgets (Quest Log's action buttons, etc.) re-add them. Harmless
        // re-work for tabs with no widgets of their own.
        if (!tabs.isEmpty()) {
            tabs.get(activeIndex).onShown();
        }
    }

    private int firstEnabledIndex() {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).enabled()) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Public so a tab can compute button layout from inside {@code onShown()} (which has no {@code
     * area} parameter of its own, unlike {@code render}/the input methods) -- Quest Log's action
     * buttons need this, since they're added directly onto the host rather than drawn by the tab
     * itself.
     */
    public Rect2i contentArea() {
        return new Rect2i(0, TAB_BAR_HEIGHT, width, Math.max(0, height - TAB_BAR_HEIGHT));
    }

    /**
     * Public wrappers over {@code Screen}'s own {@code addRenderableWidget}/{@code removeWidget} --
     * both are {@code protected}, so a tab that isn't itself a {@code Screen} subclass (every tab is
     * a plain object implementing {@link NuclyusMenu}) can't call them directly on a {@code host}
     * reference even though it's the right instance; these exist so a tab with real widgets (Quest
     * Log's action buttons, etc.) can add/remove them without this host needing to know anything
     * about what those widgets are.
     */
    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addMenuWidget(T widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public void removeMenuWidget(GuiEventListener widget) {
        removeWidget(widget);
    }

    /**
     * Real bug found live, 2026-10-17: tabs used to split {@code width} evenly regardless of each
     * title's actual text width, so a longer label (e.g. "Hunter's Journal") overran its fixed slice
     * into the neighboring tab's whenever the font rendered larger relative to the window (a higher
     * GUI Scale at the same resolution) -- confirmed by comparing two screenshots at different GUI
     * Scales, one overlapping/truncated, one fine. Each tab is now sized to its own text width
     * (plus padding); the last tab still absorbs any remaining slack so the bar still spans the full
     * width with no background gap, exactly as before.
     */
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKDROP_COLOR);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

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

        if (!tabs.isEmpty()) {
            tabs.get(activeIndex).render(graphics, contentArea(), mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.y() < TAB_BAR_HEIGHT) {
            Rect2i[] rects = tabRects();
            for (int i = 0; i < tabs.size(); i++) {
                Rect2i rect = rects[i];
                if (rect.contains((int) event.x(), (int) event.y()) && tabs.get(i).enabled()) {
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
        // Same-key toggle-close -- vanilla's own KeyMapping#consumeClick() never fires while any
        // screen is open, so the host has to check for its own keybind itself (today's standalone
        // MapScreen already needs this exact pattern for the same reason). Calls onHidden() here
        // directly, rather than overriding Screen#onClose(), since that hook is for this screen's
        // own close button/Escape handling, not a generic "something replaced this screen" signal.
        if (NuclyusModClient.OPEN_MENU.matches(event)) {
            if (!tabs.isEmpty()) {
                tabs.get(activeIndex).onHidden();
            }
            net.minecraft.client.Minecraft.getInstance().setScreen(null);
            return true;
        }
        if (!tabs.isEmpty() && tabs.get(activeIndex).keyPressed(contentArea(), event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
