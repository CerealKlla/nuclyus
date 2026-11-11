package com.github.cerealklla.nuclyus.tag;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

/** A single custom tag request awaiting admin review -- {@code requestedBy} is who asked for it,
 *  needed so {@link TagRegistry#approve} can notify them once it's accepted (see
 *  {@code TagApprovalListener}'s own doc). */
public record PendingTag(String tag, UUID requestedBy) {

    public static final Codec<PendingTag> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("tag").forGetter(PendingTag::tag),
            UUIDUtil.CODEC.fieldOf("requested_by").forGetter(PendingTag::requestedBy)
    ).apply(i, PendingTag::new));
}
