package com.github.cerealklla.nuclyus.server;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Generic version of the "generate once on first boot, persist to a JSON file via Gson+Codec, reuse
 * thereafter" pattern -- pulled out of three near-identical copies in Lyfe ({@code
 * craft.ServerRecipeStore}, {@code cook.ServerFoodRecipeStore}, {@code socket.ServerRunewordStore}).
 * By default, {@code MinecraftServer#getFile(String)} resolves to the process's own working
 * directory (a sibling of {@code server.properties}), not any single world's save path -- a
 * world/map wipe leaves a subclass's file untouched, while a real server wipe (deleting everything
 * alongside the world folders) naturally takes it with it too. This is the right scope for data that
 * is genuinely world-independent (recipes, runewords) -- exactly this class's original three
 * consumers.
 *
 * <p><b>{@code worldScoped}, added 2026-11-09</b>: real live bug, found via Arryval's starting-
 * settlement picker -- that store's own content (which real villages are nearest *this world's*
 * origin, for *this world's* seed) is fundamentally tied to one specific world, not the game
 * instance as a whole, so the instance-scoped default silently kept serving one world's discovered
 * settlement coordinates to every *later*, differently-seeded world created in the same instance/
 * install -- exactly the "nothing here, wrong coordinates entirely" a player hit after repeatedly
 * recreating their singleplayer world for testing. Passing {@code true} resolves the backing file
 * under {@code server.getWorldPath(LevelResource.ROOT)} instead, so a new world (a new save folder)
 * gets its own fresh file and fresh {@link #generate()} run, same as every other per-world save
 * naturally would. Defaults to {@code false} (the original, still-correct behavior) for every
 * existing consumer -- nothing about Lyfe's three stores changes.
 *
 * <p>A subclass provides the entry codec, how to derive each entry's key, the file name, and the
 * actual generation logic; this class owns load/save/reroll and the in-memory cache.
 */
public abstract class PersistedServerStore<K, V> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String fileName;
    private final Codec<List<V>> listCodec;
    private final Function<V, K> keyOf;
    private final boolean worldScoped;

    private volatile Path file;
    private volatile Map<K, V> values = Map.of();
    // Bumped whenever `values` is reassigned to a genuinely new generation (reroll only -- the
    // initial loadOrGenerate/mergeMissing settle into their final form before any gameplay code can
    // possibly run, so there's nothing for an external cache to miss there). Added 2026-10-28 so a
    // downstream consumer (e.g. Settlemynts' LyfeCraftingBridge#getAllRecipes) can safely cache this
    // store's mapped-out contents instead of recomputing them on every call, without going stale if
    // an admin reroll ever happens on a live server.
    private volatile int generation = 0;

    public final int generation() {
        return this.generation;
    }

    protected PersistedServerStore(String fileName, Codec<V> entryCodec, Function<V, K> keyOf) {
        this(fileName, entryCodec, keyOf, false);
    }

    /** See this class's own doc for what {@code worldScoped} means. */
    protected PersistedServerStore(String fileName, Codec<V> entryCodec, Function<V, K> keyOf, boolean worldScoped) {
        this.fileName = fileName;
        this.listCodec = entryCodec.listOf();
        this.keyOf = keyOf;
        this.worldScoped = worldScoped;
    }

    /** Builds a brand-new generated set -- called only when no on-disk file exists yet, or on a forced {@link #reroll}. */
    protected abstract Map<K, V> generate();

    /** Hook for a subclass that needs to recompute derived state whenever the underlying set changes; no-op by default. */
    protected void onLoaded() {
    }

    private Path resolveFile(MinecraftServer server) {
        return worldScoped ? server.getWorldPath(LevelResource.ROOT).resolve(fileName) : server.getFile(fileName);
    }

    public final void loadOrGenerate(MinecraftServer server) {
        Path file = resolveFile(server);
        this.file = file;
        if (Files.exists(file)) {
            values = load(file).map(loaded -> mergeMissing(loaded, file)).orElseGet(() -> generateAndSave(file));
        } else {
            values = generateAndSave(file);
        }
        onLoaded();
    }

    /**
     * Self-heals a persisted file that predates a later-added generated entry -- real report,
     * 2026-10-17: "researching Bow or Crossbow says 'nothing to learn,'" traced to the server's own
     * {@code lyfe_equipment_recipes.json} having been generated (and persisted) before ranged
     * weapons were added to {@code craft.ServerRecipeStore#generate} -- {@code loadOrGenerate} used
     * to only ever generate fresh on a genuinely absent file, so an existing-but-now-incomplete file
     * silently stayed incomplete forever; neither a world wipe nor a normal reboot ever revisits it.
     * Calls {@link #generate} once (its full, current key set) purely to find keys the loaded map is
     * missing -- never overwrites or re-rolls a key the loaded map already has, so this can't disturb
     * anything a player has already seen/adjusted to. Saves back to disk only if anything was
     * actually added.
     */
    private Map<K, V> mergeMissing(Map<K, V> loaded, Path file) {
        Map<K, V> fresh = generate();
        Map<K, V> merged = new LinkedHashMap<>(loaded);
        boolean changed = false;
        for (Map.Entry<K, V> entry : fresh.entrySet()) {
            if (!merged.containsKey(entry.getKey())) {
                merged.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        if (changed) {
            save(file, merged);
        }
        return merged;
    }

    /** Forces a brand-new random generation, overwriting the cached file. */
    public final void reroll(MinecraftServer server) {
        Path file = resolveFile(server);
        this.file = file;
        values = generateAndSave(file);
        this.generation++;
        onLoaded();
    }

    public final Optional<V> get(K key) {
        return Optional.ofNullable(values.get(key));
    }

    public final Map<K, V> all() {
        return Map.copyOf(values);
    }

    /**
     * Writes/overwrites one entry and persists immediately -- added 2026-11-03 for Audyble's Phase 5b
     * NPC identity/relationship stores, the first consumers of this class that need live incremental
     * writes at runtime rather than "generate once at boot, read-only after" ({@link #reroll} was
     * previously the only way to ever change a store's contents post-boot, and that replaces the whole
     * set). Requires {@link #loadOrGenerate}/{@link #reroll} to have run at least once first (both set
     * the backing file path) -- a store that's never been loaded has nothing to persist to yet, which
     * can't happen in practice since every store is loaded at server start before any gameplay code
     * can reach this. Bumps {@link #generation} the same way {@link #reroll} does, so a downstream
     * cache keyed on it stays correct either way.
     */
    public final synchronized void put(K key, V value) {
        Map<K, V> updated = new LinkedHashMap<>(values);
        updated.put(key, value);
        values = updated;
        this.generation++;
        save(file, updated);
    }

    /** Removes one entry and persists immediately, if present -- a no-op otherwise. Added
     *  2026-11-11 for {@code tag.TagRegistry} (approving/denying a pending tag request removes it
     *  from the pending store), the first consumer of this class that ever needs to delete a single
     *  entry rather than just upsert one. Same incremental-extension shape {@link #put} itself
     *  followed when it was added. */
    public final synchronized void remove(K key) {
        if (!values.containsKey(key)) {
            return;
        }
        Map<K, V> updated = new LinkedHashMap<>(values);
        updated.remove(key);
        values = updated;
        this.generation++;
        save(file, updated);
    }

    private Map<K, V> generateAndSave(Path file) {
        Map<K, V> generated = generate();
        save(file, generated);
        return generated;
    }

    private Optional<Map<K, V>> load(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement json = GSON.fromJson(reader, JsonElement.class);
            DataResult<List<V>> decoded = listCodec.parse(JsonOps.INSTANCE, json);
            return decoded.result().map(list -> list.stream()
                    .collect(Collectors.toMap(keyOf, v -> v, (a, b) -> a, LinkedHashMap::new)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void save(Path file, Map<K, V> toSave) {
        DataResult<JsonElement> encoded = listCodec.encodeStart(JsonOps.INSTANCE, List.copyOf(toSave.values()));
        encoded.result().ifPresent(json -> {
            try {
                Files.createDirectories(file.toAbsolutePath().getParent());
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(json, writer);
                }
            } catch (IOException ignored) {
                // Best-effort persistence -- a failed save just means regeneration next boot, not a crash.
            }
        });
    }
}
