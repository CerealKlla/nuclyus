package com.github.cerealklla.nuclyus.ping;

import java.util.List;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.nuclyus.NuclyusMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server: "here's every {@link Pingable} I can see near myself right now" -- sent by
 * {@code client.ClientPingScanner} up to 2x/sec while something is actually nearby. Deliberately
 * NOT trusted outright -- {@link PingDispatcher} re-resolves every position/id and re-checks real
 * distance server-side before dispatching anything, so a modified client reporting a false position
 * can't trigger a ping it shouldn't be able to.
 */
public record PingCandidatesPayload(List<BlockPos> blockEntityPositions, List<Integer> entityIds) implements CustomPacketPayload {

    public static final Type<PingCandidatesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NuclyusMod.MODID, "ping_candidates"));

    public static final StreamCodec<ByteBuf, PingCandidatesPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), PingCandidatesPayload::blockEntityPositions,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), PingCandidatesPayload::entityIds,
            PingCandidatesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
