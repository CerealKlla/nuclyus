package com.github.cerealklla.nuclyus.tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.cerealklla.nuclyus.server.PersistedServerStore;

import com.mojang.serialization.Codec;

import net.minecraft.server.MinecraftServer;

/**
 * The shared, server-scoped (not world-scoped) tag vocabulary -- one approved set, one pending
 * (requested-but-not-yet-reviewed) set. Explicit user spec, 2026-11-11: "we should store the list
 * of tag types in Nuclyus, and leverage them in TinyWyrlds so that we can use the same tags in
 * Blueprynts later and they'll match" -- lives here, not in any one feature mod, precisely so a
 * future Blueprynts tag feature draws from this exact same vocabulary with zero migration.
 * Flat/unnamespaced by design -- a tag like "Dark" or "Orcish" is a general descriptor any content
 * type can use, not scoped to whichever mod first requested it.
 *
 * <p>Built on two {@link PersistedServerStore} instances rather than one hand-rolled file -- {@code
 * approved} is keyed by its own tag text (identity key, since all that's ever needed is "does this
 * exist"), {@code pending} is keyed by {@link PendingTag#tag}. Each persists to its own small JSON
 * file under the server's own working directory, same scope every other {@code PersistedServerStore}
 * consumer in this suite already uses for server-wide (not per-world) data.
 */
public final class TagRegistry {

    private static final PersistedServerStore<String, String> APPROVED =
            new PersistedServerStore<>("nuclyus_approved_tags.json", Codec.STRING, s -> s) {
                @Override
                protected Map<String, String> generate() {
                    return builtInTags();
                }
            };

    private static final PersistedServerStore<String, PendingTag> PENDING =
            new PersistedServerStore<>("nuclyus_pending_tags.json", PendingTag.CODEC, PendingTag::tag) {
                @Override
                protected Map<String, PendingTag> generate() {
                    return Map.of();
                }
            };

    /** The user's own given examples, seeded on first boot (and re-added if ever missing on a
     *  later boot -- see {@link PersistedServerStore#mergeMissing} via {@link
     *  PersistedServerStore#loadOrGenerate}). Reseeding on every boot means a built-in an admin
     *  explicitly removed would reappear on restart -- accepted, same tradeoff every other
     *  `ensureX`-style bootstrap in this suite already carries. */
    private static final List<String> BUILT_IN_TAGS = List.of(
            "HasMobs", "MobFree", "Tunnel", "CitySewer", "AntNest", "Orcish", "Human", "Natural",
            "Dark", "SemiDark", "WellLit", "Abandoned", "Mineshaft");

    private TagRegistry() {
    }

    private static Map<String, String> builtInTags() {
        Map<String, String> seeded = new LinkedHashMap<>();
        for (String tag : BUILT_IN_TAGS) {
            seeded.put(tag, tag);
        }
        return seeded;
    }

    public static void loadOrGenerate(MinecraftServer server) {
        APPROVED.loadOrGenerate(server);
        PENDING.loadOrGenerate(server);
    }

    public static List<String> approvedTags() {
        return List.copyOf(APPROVED.all().keySet());
    }

    public static List<String> pendingTags() {
        return PENDING.all().values().stream().map(PendingTag::tag).toList();
    }

    public static boolean isApproved(String tag) {
        return APPROVED.get(tag).isPresent();
    }

    /** No-ops if {@code tag} is already approved or already pending (the first requester's name
     *  sticks -- see {@link PendingTag}'s own doc). */
    public static void requestCustomTag(String tag, UUID requestedBy) {
        if (isApproved(tag) || PENDING.get(tag).isPresent()) {
            return;
        }
        PENDING.put(tag, new PendingTag(tag, requestedBy));
    }

    public static void approve(String originalTag, String approvedTag) {
        UUID requestedBy = PENDING.get(originalTag).map(PendingTag::requestedBy).orElse(null);
        PENDING.remove(originalTag);
        APPROVED.put(approvedTag, approvedTag);
        TagApprovalHooks.notifyApproved(originalTag, approvedTag, requestedBy);
    }

    public static void deny(String tag) {
        PENDING.remove(tag);
    }
}
