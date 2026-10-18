package com.github.cerealklla.nuclyus.ping;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Optional convenience helper for a {@link Pingable} implementation that wants simple per-player
 * throttling -- NOT required or enforced by {@link Pingable}/{@link PingDispatcher} themselves (see
 * {@link Pingable}'s own doc for why the interface deliberately doesn't force a cooldown shape). A
 * Pingable implementation holds one of these as its own instance field and consults it however it
 * likes from inside {@code onPing}; an implementation with different needs (a global rather than
 * per-player cooldown, a variable cooldown length, no cooldown at all) is free to ignore this
 * entirely and manage its own state.
 */
public final class PingCooldown {

    private final Map<UUID, Long> lastHandledGameTime = new HashMap<>();

    /** True if {@code playerId} hasn't been handled in the last {@code cooldownTicks} (or ever). */
    public boolean isReady(UUID playerId, long nowGameTime, long cooldownTicks) {
        Long last = lastHandledGameTime.get(playerId);
        return last == null || (nowGameTime - last) >= cooldownTicks;
    }

    /** Stamps {@code playerId} as handled right now -- call after actually running the real per-ping logic, not before. */
    public void markHandled(UUID playerId, long nowGameTime) {
        lastHandledGameTime.put(playerId, nowGameTime);
    }
}
