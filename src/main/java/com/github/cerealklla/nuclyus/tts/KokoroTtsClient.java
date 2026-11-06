package com.github.cerealklla.nuclyus.tts;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Generic Kokoro-82M TTS client -- moved here from Audyble's own {@code npc.KokoroTtsClient},
 * 2026-11-06, per explicit user direction: "the LLM should be completely separate from the TTS, both
 * accessible independently from the Nuclyus API." Nuclyus is already hard-imported by every mod in
 * the suite (same reasoning as {@link com.github.cerealklla.nuclyus.llm.OllamaClient}'s own move the
 * day before) -- this lets any mod synthesize speech for a fixed, hand-authored line without needing
 * Audyble's conversation pipeline (Whisper/Ollama) at all.
 *
 * <p>Targets the <a href="https://github.com/remsky/Kokoro-FastAPI">kokoro-fastapi</a> project's
 * OpenAI-compatible {@code /v1/audio/speech} endpoint specifically, since it's the most widely used
 * community wrapper around the Kokoro model. If a different Kokoro server is used, this request/
 * response shape may need adjusting.
 */
public final class KokoroTtsClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Gson GSON = new Gson();

    private final String baseUrl;
    private final String voice;

    public KokoroTtsClient(String baseUrl, String voice) {
        this.baseUrl = baseUrl;
        this.voice = voice;
    }

    /** @return raw WAV file bytes (24kHz mono, Kokoro's native output rate) */
    public byte[] synthesize(String text) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", "kokoro");
        body.addProperty("input", text);
        body.addProperty("voice", voice);
        body.addProperty("response_format", "wav");

        // 60s, not 30 -- same reasoning as OllamaClient's own 120s bump (confirmed live 2026-11-04 a
        // local LLM call can badly exceed 30s on first real use); runs on the same background thread.
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/audio/speech"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Kokoro TTS returned HTTP " + response.statusCode());
        }
        return response.body();
    }
}
