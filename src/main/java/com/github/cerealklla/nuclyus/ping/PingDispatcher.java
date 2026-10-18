package com.github.cerealklla.nuclyus.ping;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-side half of the Pingable pipeline -- receives a {@link PingCandidatesPayload} from a
 * player's own {@code client.ClientPingScanner}, re-resolves every reported position/entity id
 * against this server's own real state, re-checks real distance (never trusts the client's claim
 * blindly), and calls {@link Pingable#onPing} on whatever survives both checks.
 */
public final class PingDispatcher {

    private PingDispatcher() {
    }

    public static void handle(PingCandidatesPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        double maxDistSq = (double) PingConstants.RADIUS_BLOCKS * PingConstants.RADIUS_BLOCKS;

        for (BlockPos pos : payload.blockEntityPositions()) {
            if (!(level.getBlockEntity(pos) instanceof Pingable pingable)) {
                continue;
            }
            double distSq = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSq > maxDistSq) {
                continue;
            }
            pingable.onPing(new PingPacket(player, Math.sqrt(distSq)));
        }

        for (int entityId : payload.entityIds()) {
            Entity entity = level.getEntity(entityId);
            if (!(entity instanceof Pingable pingable)) {
                continue;
            }
            double distSq = player.distanceToSqr(entity);
            if (distSq > maxDistSq) {
                continue;
            }
            pingable.onPing(new PingPacket(player, Math.sqrt(distSq)));
        }
    }
}
