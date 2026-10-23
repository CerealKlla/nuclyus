package com.github.cerealklla.nuclyus.menu.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Open, trust-based registry of every mod contributing tabs to the shared "K" menu -- same
 * "each contributor calls a registration method at its own setup time" shape this suite already
 * uses for Cartographyr's {@code Layer} registry, Settlemynts' {@code ZoneTypeRegistry}, etc.,
 * rather than a live {@code ModList}/{@code instanceof} scan (NeoForge has no clean hook for the
 * latter). {@link #registerContributor} is meant to be called once per mod, from that mod's own
 * client-only setup code.
 *
 * <p><b>Real production crash, 2026-10-23</b> -- a player's actual crash report (found directly on
 * this machine, not just the in-game error summary) showed every contributing mod's {@code
 * FMLClientSetupEvent} handler -- Coryspondance, Lyfe, Myssions, each calling {@link
 * #registerContributor} -- dispatched via NeoForge's {@code ModLoader.dispatchParallelEvent}, i.e.
 * concurrently, on different worker threads. {@code CONTRIBUTORS} was a plain {@code ArrayList},
 * not thread-safe for concurrent {@code add()} calls -- the classic failure mode is exactly what was
 * reported, an {@code ArrayIndexOutOfBoundsException} from two threads racing the same internal
 * backing-array resize. Didn't reproduce in a single local {@code runClient} test (timing-dependent
 * races often don't on the first try), only confirmed by reading the real crash report's own stack
 * trace. {@link CopyOnWriteArrayList} is the right shape here regardless of the race itself --
 * writes only ever happen in this brief, rare client-setup window, and every other use (){@link
 * #collectAllMenus}) is a read, which this makes trivially safe with zero added synchronization.
 */
public final class NuclyusMenuRegistry {

    private static final List<MenuContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

    private NuclyusMenuRegistry() {
    }

    public static void registerContributor(MenuContributor contributor) {
        CONTRIBUTORS.add(contributor);
    }

    /** Flattens every contributor's current tab list, sorted by {@link NuclyusMenu#order()} ascending. */
    public static List<NuclyusMenu> collectAllMenus() {
        List<NuclyusMenu> all = new ArrayList<>();
        for (MenuContributor contributor : CONTRIBUTORS) {
            all.addAll(contributor.getAllMenus());
        }
        all.sort(Comparator.comparingInt(NuclyusMenu::order));
        return all;
    }
}
