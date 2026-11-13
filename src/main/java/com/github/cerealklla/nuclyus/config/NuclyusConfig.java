package com.github.cerealklla.nuclyus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-administrator-facing tuning knobs for Nuclyus itself -- previously this foundation mod had
 * no config of its own at all. First entries, 2026-11-05: the generic Ollama LLM client ({@code
 * llm.OllamaClient}), moved here from Audyble's own now-deleted {@code config.AudybleConfig} (see
 * that class's own prior doc) per explicit user direction -- "the LLM should be completely separate
 * from the TTS, anything should be able to use the LLM if needed," not something only Audyble's
 * conversation pipeline can reach. Settlemynts' NPC identity generation and Audyble's conversation
 * replies both read these same two values now, instead of each keeping its own copy.
 */
public final class NuclyusConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> OLLAMA_BASE_URL = BUILDER
            .comment("Base URL of a local Ollama instance, e.g. http://127.0.0.1:11434 -- use a literal "
                    + "IPv4 address, not 'localhost': confirmed live 2026-11-04 that Java's HttpClient can "
                    + "fail to connect via the 'localhost' hostname when a local service binds IPv4-only "
                    + "(the loopback IPv6 ::1 resolves first and has nothing listening, and unlike curl's "
                    + "own fallback behavior, HttpClient doesn't reliably retry the IPv4 address after "
                    + "that). Leave blank to disable every LLM-dependent feature across the suite.")
            .define("ollamaBaseUrl", "http://127.0.0.1:11434");

    public static final ModConfigSpec.ConfigValue<String> OLLAMA_MODEL = BUILDER
            .comment("Ollama model name to use for any mod's LLM calls, e.g. llama3.1:8b, gemma2:9b, "
                    + "qwen2.5:7b, phi4")
            .define("ollamaModel", "llama3.1:8b");

    public static final ModConfigSpec.ConfigValue<String> KOKORO_BASE_URL = BUILDER
            .comment("Base URL of a local Kokoro-82M TTS server exposing an OpenAI-compatible "
                    + "/v1/audio/speech endpoint (e.g. the kokoro-fastapi project), e.g. "
                    + "http://127.0.0.1:8880 -- use a literal IPv4 address, not 'localhost' (see "
                    + "ollamaBaseUrl's own comment for why). Moved here from Audyble's own "
                    + "kokoroBaseUrl 2026-11-06, same reasoning as ollamaBaseUrl's own move the day "
                    + "before -- only the base URL (infrastructure: where's the server) moved; which "
                    + "voice name to use for which NPC (domain logic) stays wherever the caller lives, "
                    + "e.g. Audyble's own kokoroVoiceMale/kokoroVoiceFemale. Leave blank to disable "
                    + "every TTS-dependent feature across the suite.")
            .define("kokoroBaseUrl", "http://127.0.0.1:8880");

    public static final ModConfigSpec.BooleanValue DISABLE_VANILLA_PORTAL_AUTO_TELEPORT = BUILDER
            .comment("If true, standing in a Nether/End portal no longer automatically teleports an "
                    + "entity -- a player must right-click the portal block to cross (carrying their "
                    + "current vehicle, if mounted). Added 2026-11-13: vanilla's automatic per-tick "
                    + "crossing let a summoned mount/companion or a town guard wander into a portal "
                    + "entirely on its own with nothing tracking it, which was the root cause of both "
                    + "a summon-duplication exploit and guards going missing forever after chasing a "
                    + "target through one. Does not affect TinyWyrld entrances/exits (their own "
                    + "blocks, never vanilla's NetherPortalBlock/EndPortalBlock) or End Gateways.")
            .define("disableVanillaPortalAutoTeleport", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private NuclyusConfig() {
    }
}
