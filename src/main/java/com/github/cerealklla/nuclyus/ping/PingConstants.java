package com.github.cerealklla.nuclyus.ping;

/** Shared by {@code client.ClientPingScanner} (the scan) and {@link PingDispatcher} (the server-side re-validation) -- must stay identical on both sides. */
public final class PingConstants {

    public static final int RADIUS_BLOCKS = 20;
    public static final int SCAN_INTERVAL_TICKS = 10; // 2x/sec, per the design doc.

    private PingConstants() {
    }
}
