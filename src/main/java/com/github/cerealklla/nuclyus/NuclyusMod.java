package com.github.cerealklla.nuclyus;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Foundation mod for the suite -- hard-imported by every other mod (see the root project's own
 * CLAUDE.md for why: a shared base for cross-mod contracts, starting with the menu-contribution
 * system in {@code menu.client}). Deliberately minimal on the common side for now -- the whole
 * first feature (menu tabs) is pure client-side rendering, so there's nothing common-sourceset
 * needs to register yet.
 */
@Mod(NuclyusMod.MODID)
public class NuclyusMod {

    public static final String MODID = "nuclyus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NuclyusMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("Nuclyus common setup");
    }
}
