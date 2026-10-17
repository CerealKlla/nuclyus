package com.github.cerealklla.nuclyus.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Same poll-once-handoff shape as {@link ClientRequestSlot}, but for values that can arrive faster
 * than they're polled (e.g. rapid-fire preview images paging in) -- a queue, not a single slot, so
 * none are ever dropped.
 */
public final class ClientRequestQueue<T> {

    private final Queue<T> pending = new ConcurrentLinkedQueue<>();

    public void request(T value) {
        pending.add(value);
    }

    /** Drains and returns every pending value, in arrival order. */
    public List<T> takePending() {
        List<T> drained = new ArrayList<>();
        T next;
        while ((next = pending.poll()) != null) {
            drained.add(next);
        }
        return drained;
    }
}
