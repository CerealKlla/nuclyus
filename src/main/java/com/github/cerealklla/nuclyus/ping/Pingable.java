package com.github.cerealklla.nuclyus.ping;

/**
 * Implemented directly by any {@code BlockEntity} or {@code Entity} subclass, in any mod, that
 * wants to react to a player being within {@link PingConstants#RADIUS_BLOCKS} of it -- design doc
 * "Player-centric Pinging," 2026-10-18. No registration anywhere in Nuclyus is needed for this to
 * work: {@code client.ClientPingScanner} finds every nearby implementor with a plain
 * {@code instanceof Pingable} check against structures the client already tracks for rendering
 * (loaded chunks' own block-entity maps, the level's own loaded entity list), so a third-party mod's
 * own Pingable block/entity is picked up automatically, purely through polymorphism.
 *
 * <p>{@link #onPing} always runs server-side, dispatched up to 2x/sec per nearby online player (see
 * {@link PingDispatcher}) -- every candidate the client reports is re-resolved and re-distance-
 * checked server-side before this is ever called, so a modified client can't fake a false ping.
 *
 * <p>Deliberately carries no cooldown/throttle of its own -- different implementations legitimately
 * want different cooldown shapes (per-player vs. global, fixed vs. variable), so that's left
 * entirely to each implementation rather than forced by this contract. {@link PingCooldown} is an
 * optional convenience helper for the common per-player case, not a requirement.
 */
public interface Pingable {

    void onPing(PingPacket ping);
}
