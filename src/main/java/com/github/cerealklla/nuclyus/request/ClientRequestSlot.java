package com.github.cerealklla.nuclyus.request;

import java.util.Optional;

/**
 * Generic poll-once handoff for a server-sent value that only a genuinely client-only tick
 * listener may act on -- the common pattern behind every {@code Client*Request*} bridge class in
 * this suite (e.g. Settlemynts' {@code founding.ClientFoundingRequests}, Kyt's {@code
 * ClientKytRequests}): a mod's common {@code *Mod#registerPayloads} handler (which must stay
 * harmless to class-load on a dedicated server) calls {@link #request} with no client-only import
 * anywhere in that call chain, and the mod's own {@code *ModClient}'s client tick handler is the
 * only place that calls {@link #takePending} and actually touches {@code Minecraft}/{@code Screen}.
 */
public final class ClientRequestSlot<T> {

    private volatile T pending;

    public void request(T value) {
        pending = value;
    }

    /** Clears and returns the pending value, if any -- a poll-once handoff, not a persistent state. */
    public Optional<T> takePending() {
        T value = pending;
        pending = null;
        return Optional.ofNullable(value);
    }
}
