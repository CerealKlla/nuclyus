package com.github.cerealklla.nuclyus.chunkload;

import java.util.UUID;

/**
 * A structured, {@code toString}-friendly key identifying one logical reason a mod wants a set of
 * chunks kept loaded -- deliberately not a bare {@link UUID}, so a leaked/stuck claim is
 * identifiable in diagnostics (e.g. {@code settlemynts:midnight_wake:<uuid>} vs.
 * {@code settlemynts:proximity_wake:<uuid>} for the same settlement never collide and are both
 * legible if something needs debugging). See {@link ChunkClaims} for the real design rationale.
 */
public record ChunkClaimId(String ownerModId, String reason, UUID instanceId) {

    @Override
    public String toString() {
        return ownerModId + ":" + reason + ":" + instanceId;
    }
}
