package com.github.cerealklla.nuclyus.ping;

/**
 * Implemented directly by any {@code BlockEntity} or {@code Entity} subclass, in any mod, that
 * wants to react to a player being nearby -- design doc "Player-centric Pinging," 2026-10-18. No
 * registration anywhere in Nuclyus is needed for this to work: {@code
 * client.ClientPingableCache} keeps a small, always-current set of every loaded implementor via a
 * plain {@code instanceof Pingable} check, updated purely by real chunk/entity load-unload events
 * (never re-derived from a fresh scan), so a third-party mod's own Pingable block/entity is picked
 * up automatically, purely through polymorphism.
 *
 * <p><b>The Pingable itself defines its own ping radius</b> via {@link #radiusBlocks} -- explicit
 * design call, confirmed again 2026-11-06 after a real regression where a consumer (Arryval's exit-
 * portal marker) had to work around a hardcoded shared radius by self-filtering inside {@code
 * onPing}. There is no single radius value that's actually "correct" across every Pingable -- a
 * road marker's traveler-buff range, a shop's chat-menu prompt range, and a portal's proactive-popup
 * range are all genuinely different distances for genuinely different reasons, so this is never
 * meant to be left at some shared number -- confirmed twice now by the same real bug recurring:
 * Roadway Stakes' own traveler buff once fired from over 10 blocks away specifically because a
 * shared value stood in for a real per-feature distance, exactly the mistake this doc is now
 * repeating itself to prevent a third time. There's deliberately no scan-ceiling constant to weigh
 * against here either (an earlier version of this fix tried exactly that, then removed it the same
 * session) -- {@code client.ClientPingableCache} has nothing left to bound, since it's driven by
 * real load/unload events rather than a derived search box, so a real Pingable's own radius is the
 * only distance that matters anywhere in this pipeline. {@link #radiusBlocks}'s own default is
 * deliberately tiny (effectively "must be standing right on top of it"), so an implementation that
 * forgets to override this fails safe (almost never triggers) rather than silently inheriting some
 * shared "reasonable-sounding" distance that happens to be wrong for it. Every real feature must
 * override this with whatever distance actually matches its own interaction design -- there is no
 * "normal" radius to fall back on.
 *
 * <p>{@link #onPing} always runs server-side, dispatched up to 2x/sec per nearby online player (see
 * {@link PingDispatcher}) -- every candidate the client reports is re-resolved and re-distance-
 * checked server-side (against this same {@link #radiusBlocks}) before this is ever called, so a
 * modified client can't fake a false ping or a false (larger) radius.
 *
 * <p>Deliberately carries no cooldown/throttle of its own -- different implementations legitimately
 * want different cooldown shapes (per-player vs. global, fixed vs. variable), so that's left
 * entirely to each implementation rather than forced by this contract. {@link PingCooldown} is an
 * optional convenience helper for the common per-player case, not a requirement.
 */
public interface Pingable {

    void onPing(PingPacket ping);

    /** This Pingable's own real trigger radius -- see this interface's own doc. Deliberately tiny by
     *  default (fails safe) -- always override with the real distance this feature actually needs. */
    default double radiusBlocks() {
        return 1.0;
    }
}
