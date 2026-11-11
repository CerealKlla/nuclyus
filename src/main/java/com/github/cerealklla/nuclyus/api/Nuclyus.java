package com.github.cerealklla.nuclyus.api;

import java.io.IOException;
import java.util.List;

import com.github.cerealklla.nuclyus.config.NuclyusConfig;
import com.github.cerealklla.nuclyus.llm.OllamaClient;
import com.github.cerealklla.nuclyus.tts.KokoroTtsClient;
import com.github.cerealklla.nuclyus.welcome.WelcomeBookContent;

import net.minecraft.world.item.ItemStack;

/**
 * Stable cross-mod facade for Nuclyus's two independent AI capabilities -- the LLM (Ollama) and TTS
 * (Kokoro) clients, each usable on its own with no dependency on the other. Added 2026-11-06, per
 * explicit user direction: "the LLM should be completely separate from the TTS, both accessible
 * independently from the Nuclyus API." Both {@link OllamaClient}/{@link KokoroTtsClient} already had
 * zero coupling to each other; this just gives callers one conventional entry point to reach either,
 * matching every other mod's own {@code api.<ModName>} facade (e.g. {@code api.Settlemynts}, {@code
 * api.Lyfe}) instead of reaching into {@code llm}/{@code tts} package internals directly.
 */
public final class Nuclyus {

    private Nuclyus() {
    }

    /** {@code true} once an administrator has pointed {@code NuclyusConfig#OLLAMA_BASE_URL} at a real server. */
    public static boolean isLlmConfigured() {
        return OllamaClient.isConfigured();
    }

    /**
     * @param systemPrompt the character/behavior definition for this call
     * @param history      prior turns in the conversation (oldest first), excluding the system prompt -- pass {@link List#of()} for a one-shot call with no history
     * @param userMessage  the latest message to respond to
     * @return the model's reply text
     */
    public static String chat(String systemPrompt, List<OllamaClient.Turn> history, String userMessage) throws IOException, InterruptedException {
        return OllamaClient.chat(systemPrompt, history, userMessage);
    }

    /** {@code true} once an administrator has pointed {@code NuclyusConfig#KOKORO_BASE_URL} at a real server. */
    public static boolean isTtsConfigured() {
        return !NuclyusConfig.KOKORO_BASE_URL.get().isBlank();
    }

    /**
     * @param voice a Kokoro voice name (e.g. {@code am_michael}, {@code af_heart}) -- the caller resolves which voice to use, Nuclyus has no opinion on NPC gender/identity
     * @param text  the exact text to synthesize
     * @return raw WAV file bytes (24kHz mono, Kokoro's native output rate)
     */
    public static byte[] speak(String voice, String text) throws IOException, InterruptedException {
        return new KokoroTtsClient(NuclyusConfig.KOKORO_BASE_URL.get(), voice).synthesize(text);
    }

    /** Builds a fresh "Kyngdoms, A History" written book -- one section per currently-loaded mod,
     *  see {@link WelcomeBookContent}'s own doc for why this lives here rather than in any one
     *  feature mod. Added 2026-11-11 for Arryval's {@code onboarding.TutorialRecipeBootstrap},
     *  which seeds this straight into the tutorial WyrldRecipe's own common-loot pool. */
    public static ItemStack createWelcomeBook() {
        return WelcomeBookContent.build();
    }
}
