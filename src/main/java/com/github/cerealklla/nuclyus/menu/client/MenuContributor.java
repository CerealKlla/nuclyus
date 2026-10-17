package com.github.cerealklla.nuclyus.menu.client;

import java.util.List;

/**
 * What a mod implements to add tabs to the shared "K" menu ("GetAllMenus()" per the user's own
 * spec) -- register an instance via {@link NuclyusMenuRegistry#registerContributor} from your own
 * client setup (a {@code *ModClient} class or equivalent), never from common-sourceset code, since
 * {@link NuclyusMenu} deals entirely in client-only rendering types.
 */
public interface MenuContributor {

    /**
     * Every tab this mod wants to show, fresh each call -- called once every time the menu opens,
     * so a tab with per-session state (scroll position, pan/zoom, etc.) should be a brand new
     * instance each time, the same way every full-screen UI in this suite already constructs itself
     * fresh on each open.
     */
    List<NuclyusMenu> getAllMenus();
}
