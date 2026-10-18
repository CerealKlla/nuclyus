package com.github.cerealklla.nuclyus.ping;

import net.minecraft.server.level.ServerPlayer;

/** Handed to {@link Pingable#onPing} -- which player triggered this ping, and the real server-resolved distance from them to the pinged object. */
public record PingPacket(ServerPlayer player, double distanceToPlayer) {
}
