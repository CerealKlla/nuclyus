package com.github.cerealklla.nuclyus.ping;

/**
 * Shared by {@code client.ClientPingScanner} (the scan) and {@link PingDispatcher} (the server-side
 * re-validation) -- must stay identical on both sides.
 *
 * <p><b>Not "the" ping radius/interval for anything</b> -- explicit, repeated user correction (first
 * at initial design, then again 2026-11-06 after the exact same mistake recurred: a shared value here
 * being treated as -- or defaulted to as -- a real per-feature distance, which once visibly let
 * Roadway Stakes' own traveler buff trigger from over 10 blocks away when it's meant to be a tight
 * few-block range). Real distances are always a property of the specific {@link Pingable} (2 blocks
 * for a Roadway Stake, 4 for Arryval's exit-portal popup, 10/20/50+ for others -- see {@link
 * Pingable#radiusBlocks}), and real dispatch cadence is always that same implementation's own choice
 * (see {@link PingCooldown}, deliberately not enforced here either).
 *
 * <p>An earlier version of this class also carried a {@code SCAN_RADIUS_BLOCKS}, an outer search-box
 * ceiling {@code client.ClientPingScanner} used to bound its own chunk/entity enumeration -- removed
 * entirely 2026-11-06, same session: there's no number that's ever correct there either (any fixed
 * ceiling just becomes a second, smaller failure point on top of the real one already imposed by the
 * client's own render distance -- anything further isn't loaded client-side to find at all,
 * regardless of what number is picked). The real fix was to stop deriving a search box at all --
 * {@code client.ClientPingableCache} now maintains a small, always-current set of every loaded
 * {@link Pingable}, kept in sync purely by real chunk/entity load-unload events, so the scan has
 * nothing left to bound in the first place.
 */
public final class PingConstants {

    public static final int SCAN_INTERVAL_TICKS = 10; // 2x/sec -- the scan's own re-check cadence, not any Pingable's dispatch rate.

    private PingConstants() {
    }
}
