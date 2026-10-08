package in.raahi.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GeminiAiMechanicProvider implements AiMechanicProvider {
    private static final Logger log = LoggerFactory.getLogger(GeminiAiMechanicProvider.class);

    // Raahi's AI Mechanic system prompt: general roadside guidance only, never a confident
    // diagnosis, always defers to a real mechanic for anything beyond basic troubleshooting.
    private static final String SYSTEM_PROMPT = """
        You are Raahi's AI Mechanic assistant, helping Indian drivers with roadside vehicle
        problems over chat. Give general, practical guidance for common issues (flat tyre,
        dead battery, overheating, won't start, etc). Never claim certainty about what's
        actually wrong with their specific vehicle — you cannot see or inspect it. For
        anything involving brakes, steering, fuel leaks, smoke, fire, or that you are not
        confident is safe to diagnose over chat, clearly recommend they stop driving and
        contact a real mechanic or use Raahi's Roadside Help / SOS feature. Keep replies
        short and practical, in simple English (or Hindi if the user writes in Hindi).
        """;

    public static final String FRIENDLY_UNAVAILABLE_MESSAGE =
            "AI Mechanic is temporarily busy. Please try again in a moment, or use Roadside Help / Nearby Mechanics.";

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 600;

    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;

    public GeminiAiMechanicProvider(
            @Value("${raahi.ai.gemini-api-key:}") String apiKey,
            @Value("${raahi.ai.gemini-model:gemini-2.0-flash}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public String providerName() { return "gemini"; }

    @Override
    public String complete(List<ChatTurn> history) throws AiUnavailableException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiUnavailableException(FRIENDLY_UNAVAILABLE_MESSAGE);
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";

        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatTurn turn : history) {
            String geminiRole = "assistant".equals(turn.role()) ? "model" : "user";
            contents.add(Map.of("role", geminiRole, "parts", List.of(Map.of("text", turn.content()))));
        }

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                "contents", contents
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        long backoffMs = INITIAL_BACKOFF_MS;
        Throwable lastTransientError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                JsonNode response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), JsonNode.class);
                if (response == null) {
                    throw new AiUnavailableException(FRIENDLY_UNAVAILABLE_MESSAGE);
                }
                JsonNode textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                    throw new AiUnavailableException(FRIENDLY_UNAVAILABLE_MESSAGE);
                }
                return textNode.asText();
            } catch (AiUnavailableException e) {
                throw e;
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                boolean isTransient = (status == 429 || status == 500 || status == 502 || status == 503 || status == 504);
                if (isTransient && attempt < MAX_ATTEMPTS) {
                    log.warn("Gemini attempt {}/{} failed with transient HTTP status {}. Retrying in {}ms...",
                            attempt, MAX_ATTEMPTS, status, backoffMs);
                    lastTransientError = e;
                    sleepQuietly(backoffMs);
                    backoffMs *= 2;
                    continue;
                }
                // Never log the Gemini API key or raw sensitive error payload
                log.error("Gemini request failed: HTTP {}: {}", status, e.getStatusText());
                throw new AiUnavailableException(FRIENDLY_UNAVAILABLE_MESSAGE, e);
            } catch (ResourceAccessException e) {
                // Network timeout or transient connection drop
                if (attempt < MAX_ATTEMPTS) {
                    log.warn("Gemini attempt {}/{} failed with network timeout. Retrying in {}ms...",
                            attempt, MAX_ATTEMPTS, backoffMs);
                    lastTransientError = e;
                    sleepQuietly(backoffMs);
                    backoffMs *= 2;
                    continue;
                }
                log.error("Gemini network timeout: {}", e.getMessage());
                throw new AiUnavailableException(FRIENDLY_UNAVAILABLE_MESSAGE, e);
            } catch (Exception e) {
                log.error("Gemini request failed: {}: {}", e.getClass().getSimpleName(), e.getMessage());
                throw new AiUnavailableException(FRIENDLY_UNAVAILABLE_MESSAGE, e);
            }
        }

        throw new AiUnavailableException(FRIENDLY_UNAVAILABLE_MESSAGE, lastTransientError);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
