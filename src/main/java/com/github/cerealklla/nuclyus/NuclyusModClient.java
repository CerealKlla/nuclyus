package com.github.cerealklla.nuclyus;

import com.github.cerealklla.nuclyus.menu.client.NuclyusMenuScreen;
import com.github.cerealklla.nuclyus.ping.client.ClientPingScanner;
import com.github.cerealklla.nuclyus.statbar.client.StatBarWorldOverlay;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * "K" opens/toggles the shared menu ({@link NuclyusMenuScreen}) -- moved here from Lyfe's own
 * former K=Skills/M=Map keybinds as part of the menu-consolidation feature (see the root project's
 * own CLAUDE.md). Same open/toggle shape every other full-screen UI keybind in this suite already
 * uses.
 */
@Mod(value = NuclyusMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NuclyusMod.MODID, value = Dist.CLIENT)
public class NuclyusModClient {

    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.nuclyus.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyMapping.Category.MISC);

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(NuclyusMod.MODID, "stat_bar_overlay"), new StatBarWorldOverlay());
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_MENU.consumeClick()) {
            if (minecraft.player == null) {
                continue;
            }
            if (minecraft.screen instanceof NuclyusMenuScreen) {
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new NuclyusMenuScreen());
            }
        }
        ClientPingScanner.tick();
    }
}
