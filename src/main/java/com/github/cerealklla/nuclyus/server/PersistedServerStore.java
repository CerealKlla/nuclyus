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

/**
 * Generic version of the "generate once on first boot, persist to a JSON file in the server's own
 * working directory via Gson+Codec, reuse thereafter" pattern -- pulled out of three near-identical
 * copies in Lyfe ({@code craft.ServerRecipeStore}, {@code cook.ServerFoodRecipeStore}, {@code
 * socket.ServerRunewordStore}). {@code MinecraftServer#getFile(String)} resolves to the process's
 * own working directory (a sibling of {@code server.properties}), not any single world's save path
 * -- a world/map wipe leaves a subclass's file untouched, while a real server wipe (deleting
 * everything alongside the world folders) naturally takes it with it too.
 *
 * <p>A subclass provides the entry codec, how to derive each entry's key, the file name, and the
 * actual generation logic; this class owns load/save/reroll and the in-memory cache.
 */
public abstract class PersistedServerStore<K, V> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String fileName;
    private final Codec<List<V>> listCodec;
    private final Function<V, K> keyOf;

    private volatile Map<K, V> values = Map.of();

    protected PersistedServerStore(String fileName, Codec<V> entryCodec, Function<V, K> keyOf) {
        this.fileName = fileName;
        this.listCodec = entryCodec.listOf();
        this.keyOf = keyOf;
    }

    /** Builds a brand-new generated set -- called only when no on-disk file exists yet, or on a forced {@link #reroll}. */
    protected abstract Map<K, V> generate();

    /** Hook for a subclass that needs to recompute derived state whenever the underlying set changes; no-op by default. */
    protected void onLoaded() {
    }

    public final void loadOrGenerate(MinecraftServer server) {
        Path file = server.getFile(fileName);
        values = Files.exists(file) ? load(file).orElseGet(() -> generateAndSave(file)) : generateAndSave(file);
        onLoaded();
    }

    /** Forces a brand-new random generation, overwriting the cached file. */
    public final void reroll(MinecraftServer server) {
        values = generateAndSave(server.getFile(fileName));
        onLoaded();
    }

    public final Optional<V> get(K key) {
        return Optional.ofNullable(values.get(key));
    }

    public final Map<K, V> all() {
        return Map.copyOf(values);
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
