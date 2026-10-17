package com.github.cerealklla.nuclyus.menu.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Open, trust-based registry of every mod contributing tabs to the shared "K" menu -- same
 * "each contributor calls a registration method at its own setup time" shape this suite already
 * uses for Cartographyr's {@code Layer} registry, Settlemynts' {@code ZoneTypeRegistry}, etc.,
 * rather than a live {@code ModList}/{@code instanceof} scan (NeoForge has no clean hook for the
 * latter). {@link #registerContributor} is meant to be called once per mod, from that mod's own
 * client-only setup code.
 */
public final class NuclyusMenuRegistry {

    private static final List<MenuContributor> CONTRIBUTORS = new ArrayList<>();

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
