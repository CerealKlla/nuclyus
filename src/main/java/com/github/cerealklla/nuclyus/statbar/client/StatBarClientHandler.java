package com.github.cerealklla.nuclyus.statbar.client;

import java.util.ArrayList;
import java.util.List;

import com.github.cerealklla.nuclyus.NuclyusMod;
import com.github.cerealklla.nuclyus.statbar.StatBarOwner;
import com.github.cerealklla.nuclyus.statbar.StatBarProvider;
import com.github.cerealklla.nuclyus.statbar.StatBarRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/**
 * Floating stat bars for any {@link StatBarOwner} (explicit user request, 2026-10-21: "create
 * custom health and mana indicators to float over the NPCs, the nametag style isn't sufficient,
 * and I want to be able to have the guards have names in the future. All entities should be
 * capable of having these health/mana representations").
 *
 * <p><b>Second design, 2026-10-21</b> -- the first attempt stashed a snapshot onto the render
 * state during {@code RenderNameTagEvent.CanRender} (which hands over the live entity) and read it
 * back during {@code RenderLivingEvent.Post} (which hands over the drawing tools) assuming both
 * events see the same render-state object for one entity in one frame. Confirmed live via
 * diagnostic logging that this assumption is wrong for these entities' renderer chain
 * ({@code HumanoidMobRenderer}/{@code VillagerRenderer}) -- the two events' render-state objects
 * never share an identity (different {@code System.identityHashCode} every single time), so
 * whatever got stashed in {@code CanRender} was never there for {@code Post} to read. Rather than
 * chase why extraction produces two different objects for this renderer chain, this version
 * doesn't rely on cross-event object identity at all: {@link #onCanRenderNameTag} is the *only*
 * hook, and it directly overrides the nametag's own text content via {@link
 * RenderNameTagEvent.CanRender#setContent}/{@code #setCanRender} -- the same mechanism that
 * already reliably displays a real custom name for any entity in vanilla, so there's no new
 * drawing/attachment/camera-orientation code to get wrong.
 *
 * <p>The real name is prepended when one exists (via {@link RenderNameTagEvent.CanRender#getOriginalContent},
 * not overwritten) -- {@code settlemynts.npc.NpcHealthBar} (replaced by this class) had to choose
 * between a name and a health bar because it hijacked {@code Entity#setCustomName} itself; this
 * class never touches the entity's real name at all, only what the client *displays* for it, so a
 * future real name and these bars can coexist on the same line with no conflict.
 */
@EventBusSubscriber(modid = NuclyusMod.MODID, value = Dist.CLIENT)
public final class StatBarClientHandler {

    private static final int SEGMENTS = 10;
    private static final char FILLED = '█';
    private static final char EMPTY = '░';

    private StatBarClientHandler() {
    }

    @SubscribeEvent
    static void onCanRenderNameTag(RenderNameTagEvent.CanRender event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !(living instanceof StatBarOwner)) {
            return;
        }
        List<Component> bars = new ArrayList<>();
        for (StatBarProvider provider : StatBarRegistry.providers()) {
            if (provider.isApplicable(living)) {
                bars.add(barText(provider, living));
            }
        }
        if (bars.isEmpty()) {
            return; // Nothing to add -- leave vanilla's own default nametag behavior untouched.
        }
        MutableComponent combined = event.getOriginalContent() != null
                ? event.getOriginalContent().copy()
                : Component.empty();
        boolean hadRealContent = !combined.getString().isEmpty();
        for (Component bar : bars) {
            if (hadRealContent) {
                combined.append(Component.literal("  "));
            }
            combined.append(bar);
            hadRealContent = true;
        }
        event.setContent(combined);
        event.setCanRender(TriState.TRUE);
    }

    private static Component barText(StatBarProvider provider, LivingEntity entity) {
        float max = provider.max(entity);
        float fraction = max > 0.0F ? Math.max(0.0F, Math.min(1.0F, provider.current(entity) / max)) : 0.0F;
        int filled = Math.round(fraction * SEGMENTS);
        StringBuilder text = new StringBuilder(SEGMENTS);
        for (int i = 0; i < SEGMENTS; i++) {
            text.append(i < filled ? FILLED : EMPTY);
        }
        MutableComponent component = Component.literal(text.toString());
        return component.withStyle(style -> style.withColor(provider.color()));
    }
}
