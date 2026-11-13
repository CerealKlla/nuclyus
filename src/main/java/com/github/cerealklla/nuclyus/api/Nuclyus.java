package com.github.cerealklla.nuclyus.api;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.github.cerealklla.nuclyus.config.NuclyusConfig;
import com.github.cerealklla.nuclyus.llm.OllamaClient;
import com.github.cerealklla.nuclyus.portal.PortalAutoTeleportBypass;
import com.github.cerealklla.nuclyus.portal.PortalCrossing;
import com.github.cerealklla.nuclyus.tag.TagApprovalHooks;
import com.github.cerealklla.nuclyus.tag.TagApprovalListener;
import com.github.cerealklla.nuclyus.tag.TagRegistry;
import com.github.cerealklla.nuclyus.tts.KokoroTtsClient;
import com.github.cerealklla.nuclyus.welcome.WelcomeBookContent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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

    /**
     * The shared tag vocabulary -- see {@code tag.TagRegistry}'s own doc for why this lives here
     * rather than in any one feature mod. Added 2026-11-11 for TinyWyrlds' WyrldPrint Tags feature,
     * explicit user spec: "we should store the list of tag types in Nuclyus, and leverage them in
     * TinyWyrlds so that we can use the same tags in Blueprynts later and they'll match."
     */
    public static List<String> approvedTags() {
        return TagRegistry.approvedTags();
    }

    /** Tag text only, for display/edit purposes -- see {@code requestCustomTag} for who requested each one. */
    public static List<String> pendingTags() {
        return TagRegistry.pendingTags();
    }

    public static boolean isTagApproved(String tag) {
        return TagRegistry.isApproved(tag);
    }

    /** No-ops if {@code tag} is already approved or already pending. */
    public static void requestCustomTag(String tag, UUID requestedBy) {
        TagRegistry.requestCustomTag(tag, requestedBy);
    }

    /** {@code approvedTag} may equal {@code originalTag} (approved as-is) or differ (an admin
     *  corrected it) -- notifies every registered {@link TagApprovalListener} either way, so a
     *  caller like TinyWyrlds can retroactively rewrite its own already-tagged content and/or
     *  notify the requester. Nuclyus does nothing else with the approval itself. */
    public static void approveTag(String originalTag, String approvedTag) {
        TagRegistry.approve(originalTag, approvedTag);
    }

    /** Removes {@code tag} from the pending queue only -- no listener callback, nothing retroactive. */
    public static void denyTag(String tag) {
        TagRegistry.deny(tag);
    }

    /** A caller registers from its own constructor, gated behind {@code ModList.isLoaded} like every
     *  other optional-consumer registration in this suite -- registration order across mods doesn't
     *  matter. */
    public static void registerTagApprovalListener(TagApprovalListener listener) {
        TagApprovalHooks.register(listener);
    }

    /**
     * Manually performs a real Nether/End portal crossing for {@code entity} at {@code portalPos} --
     * the replacement for vanilla's own automatic per-tick crossing, which {@code
     * NuclyusConfig#DISABLE_VANILLA_PORTAL_AUTO_TELEPORT} disables (see {@code
     * portal.VanillaPortalAutoTeleportListener}'s own doc). Added 2026-11-13 for Settlemynts'
     * {@code guardhouse.GuardReturnToDimensionGoal} to recover a guard once it's snapped onto a
     * portal block, and used the same way by this mod's own right-click-to-cross feature.
     *
     * @return {@code true} if a crossing (or, for an unseen End Portal, the end-credits sequence)
     *         was actually performed
     */
    public static boolean attemptPortalCrossing(ServerLevel level, Entity entity, BlockPos portalPos) {
        return PortalCrossing.attempt(level, entity, portalPos);
    }

    /**
     * Runs {@code action} with {@code VanillaPortalAutoTeleportListener}'s own cancellation
     * suppressed, so a deliberate cross-dimension {@code Entity#teleport(TeleportTransition)} call
     * inside it is never wrongly cancelled just because the entity happens to be standing on a real
     * portal block at that moment -- see {@code portal.PortalAutoTeleportBypass}'s own doc. Added
     * 2026-11-13 for Caryvan's {@code interaction.WagonMountPortalFollowListener}, whose team
     * mount/wagon are routinely still overlapping the portal plane they're being towed through at
     * the exact moment it teleports them directly.
     */
    public static void runPortalCrossingBypassed(Runnable action) {
        PortalAutoTeleportBypass.run(action);
    }
}
