package com.github.cerealklla.nuclyus.statbar;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Open registration, no pairwise dependency needed -- same shape as {@code menu.client.NuclyusMenuRegistry}. */
public final class StatBarRegistry {

    private static final List<StatBarProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private StatBarRegistry() {
    }

    public static void register(StatBarProvider provider) {
        PROVIDERS.add(provider);
    }

    public static List<StatBarProvider> providers() {
        return PROVIDERS;
    }
}
