package com.github.cerealklla.nuclyus.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import com.github.cerealklla.nuclyus.config.NuclyusConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Generic Ollama chat-completion client -- moved here from Audyble's own {@code npc.OllamaClient},
 * 2026-11-05, per explicit user direction: "the LLM should be completely separate from the TTS,
 * anything should be able to use the LLM if needed... Audyble is purely for conveying sound and
 * text." Nuclyus is already hard-imported by every mod in the suite, so this is the natural shared
 * home -- Settlemynts' NPC identity generation and Audyble's conversation replies both call this
 * same client now, with no dependency edge between those two mods needed for it.
 *
 * <p>Targets Ollama's own {@code /api/chat} endpoint (non-streaming, {@code "stream": false}) --
 * simpler to consume than the streaming form for a single request/response turn, at the cost of
 * waiting for the full reply before TTS can start.
 *
 * <p><b>Assumption flagged for verification</b>: this targets a stock Ollama install's default API
 * shape as of this writing. If the user's actual Ollama version/config differs, the request/response
 * JSON shape below may need adjusting.</p>
 */
public final class OllamaClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Gson GSON = new Gson();

    public record Turn(String role, String content) {
    }

    private OllamaClient() {
    }

    /** {@code true} once an administrator has pointed {@code NuclyusConfig#OLLAMA_BASE_URL} at a real server -- blank (the default) means every LLM-dependent feature across the suite should skip generation entirely rather than fail loudly. */
    public static boolean isConfigured() {
        return !NuclyusConfig.OLLAMA_BASE_URL.get().isBlank();
    }

    /**
     * @param systemPrompt the character/behavior definition for this call
     * @param history      prior turns in the conversation (oldest first), excluding the system prompt -- pass {@link List#of()} for a one-shot call with no history (e.g. NPC birth generation)
     * @param userMessage  the latest message to respond to
     * @return the model's reply text
     */
    public static String chat(String systemPrompt, List<Turn> history, String userMessage) throws IOException, InterruptedException {
        String baseUrl = NuclyusConfig.OLLAMA_BASE_URL.get();
        String model = NuclyusConfig.OLLAMA_MODEL.get();

        JsonArray messages = new JsonArray();
        messages.add(message("system", systemPrompt));
        for (Turn turn : history) {
            messages.add(message(turn.role(), turn.content()));
        }
        messages.add(message("user", userMessage));

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("stream", false);
        // Confirmed live 2026-11-04 (Audyble): Ollama unloads a model from VRAM after its default
        // 5-minute idle timeout, and reloading it (several GB off disk) adds a real multi-second delay
        // to the next reply, even on a fast GPU -- "-1" keeps it loaded indefinitely once any caller has
        // made one request, trading a small amount of permanently-held VRAM for consistently fast
        // replies instead of a slow one every time a caller returns after a short gap.
        body.addProperty("keep_alive", -1);

        // 120s, not 30 -- confirmed live 2026-11-04 that a real local Ollama call can genuinely take
        // this long (a model's first real inference after being freshly pulled has to load several GB
        // off disk into memory, worse under CPU-only inference or when a second concurrent Ollama call
        // is competing for the same instance). Every known caller already runs this off the main server
        // thread (Audyble's own pipeline executor, Settlemynts' birth-generation executor), so a long
        // wait here has no effect on the game itself.
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.getAsJsonObject("message").get("content").getAsString();
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }
}
