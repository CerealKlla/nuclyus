package com.github.cerealklla.nuclyus;

import com.github.cerealklla.nuclyus.ping.PingCandidatesPayload;
import com.github.cerealklla.nuclyus.ping.PingDispatcher;
import com.github.cerealklla.nuclyus.statbar.HealthStatBarProvider;
import com.github.cerealklla.nuclyus.statbar.StatBarRegistry;
import com.github.cerealklla.nuclyus.trade.ModTradeComponents;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

/**
 * Foundation mod for the suite -- hard-imported by every other mod (see the root project's own
 * CLAUDE.md for why: a shared base for cross-mod contracts, starting with the menu-contribution
 * system in {@code menu.client}). First real common-sourceset registration, 2026-10-18: the {@code
 * ping} package's networking (see {@link PingDispatcher}/{@code ping.client.ClientPingScanner}).
 */
@Mod(NuclyusMod.MODID)
public class NuclyusMod {

    public static final String MODID = "nuclyus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NuclyusMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
        ModTradeComponents.DATA_COMPONENTS.register(modEventBus);
    }

    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("Nuclyus common setup");
        StatBarRegistry.register(new HealthStatBarProvider());
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(PingCandidatesPayload.TYPE, PingCandidatesPayload.STREAM_CODEC,
                (payload, context) -> PingDispatcher.handle(payload, context));
    }
}
