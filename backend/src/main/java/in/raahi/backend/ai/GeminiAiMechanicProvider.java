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

    private static final int MAX_ATTEMPTS = 2;
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
        factory.setReadTimeout(20000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public String providerName() { return "gemini"; }

    List<Map<String, Object>> buildGeminiContents(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> contents = new ArrayList<>();
        String currentRole = null;
        StringBuilder currentText = new StringBuilder();

        for (ChatTurn turn : history) {
            if (turn == null || turn.content() == null || turn.content().isBlank()) {
                continue;
            }
            String geminiRole = "assistant".equals(turn.role()) ? "model" : "user";

            if (geminiRole.equals(currentRole)) {
                // Merge consecutive turns with the same role to strictly obey Gemini alternating roles rule
                currentText.append("\n\n").append(turn.content().trim());
            } else {
                if (currentRole != null && !currentText.isEmpty()) {
                    contents.add(Map.of("role", currentRole, "parts", List.of(Map.of("text", currentText.toString()))));
                }
                currentRole = geminiRole;
                currentText = new StringBuilder(turn.content().trim());
            }
        }

        if (currentRole != null && !currentText.isEmpty()) {
            contents.add(Map.of("role", currentRole, "parts", List.of(Map.of("text", currentText.toString()))));
        }

        // Gemini REST API rule: Conversation history MUST start with a 'user' turn
        while (!contents.isEmpty() && "model".equals(contents.get(0).get("role"))) {
            contents.remove(0);
        }

        return contents;
    }

    @Override
    public String complete(List<ChatTurn> history) throws AiUnavailableException {
        String corrId = java.util.UUID.randomUUID().toString().substring(0, 8);
        long startMs = System.currentTimeMillis();

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[AI-{}] Gemini API key is missing or blank.", corrId);
            throw new AiUnavailableException(ErrorCategory.CONFIG_ERROR,
                    "AI Mechanic service is temporarily misconfigured. Please try again later.", null, null);
        }

        List<Map<String, Object>> contents = buildGeminiContents(history);
        if (contents.isEmpty()) {
            log.warn("[AI-{}] No valid content found in message history.", corrId);
            throw new AiUnavailableException(ErrorCategory.MALFORMED_RESPONSE,
                    "No message content to process. Please enter your vehicle symptom.", null, null);
        }

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                "contents", contents,
                "generationConfig", Map.of("maxOutputTokens", 600, "temperature", 0.2)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        long backoffMs = INITIAL_BACKOFF_MS;
        Throwable lastTransientError = null;
        String currentModel = this.model;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long attemptStart = System.currentTimeMillis();
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + currentModel + ":generateContent";
            try {
                log.info("[AI-{}] Sending request to Gemini (attempt {}/{}, model {}, turns {})",
                        corrId, attempt, MAX_ATTEMPTS, currentModel, contents.size());
                JsonNode response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), JsonNode.class);
                long latency = System.currentTimeMillis() - attemptStart;
                if (response == null) {
                    log.error("[AI-{}] Gemini response body is null after {}ms", corrId, latency);
                    throw new AiUnavailableException(ErrorCategory.MALFORMED_RESPONSE,
                            "AI Mechanic returned an empty response. Please try again.", null, null);
                }
                JsonNode textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                    log.error("[AI-{}] Gemini text missing in response after {}ms", corrId, latency);
                    throw new AiUnavailableException(ErrorCategory.MALFORMED_RESPONSE,
                            "AI Mechanic response could not be parsed. Please try again.", null, null);
                }
                log.info("[AI-{}] Gemini responded successfully in {}ms (total elapsed: {}ms)",
                        corrId, latency, System.currentTimeMillis() - startMs);
                return textNode.asText();
            } catch (AiUnavailableException e) {
                throw e;
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                long latency = System.currentTimeMillis() - attemptStart;
                String errorBody = e.getResponseBodyAsString();
                String sanitizedBody = errorBody.replaceAll("[\\r\\n]+", " ");
                log.error("[AI-{}] Gemini request failed: HTTP {} after {}ms: {}",
                        corrId, status, latency, sanitizedBody);

                // Automatic model fallback if model is not found (404) or unsupported
                if (status == 404 && !"gemini-1.5-flash".equals(currentModel)) {
                    log.warn("[AI-{}] Model {} returned 404 Not Found. Falling back to gemini-1.5-flash...", corrId, currentModel);
                    currentModel = "gemini-1.5-flash";
                    continue;
                }

                boolean isTransient = (status == 429 || status == 503 || status == 504);
                if (isTransient && attempt < MAX_ATTEMPTS) {
                    log.warn("[AI-{}] Gemini attempt {}/{} failed with transient HTTP status {} in {}ms. Retrying in {}ms...",
                            corrId, attempt, MAX_ATTEMPTS, status, latency, backoffMs);
                    lastTransientError = e;
                    sleepQuietly(backoffMs);
                    backoffMs *= 2;
                    continue;
                }

                if (status == 429) {
                    throw new AiUnavailableException(ErrorCategory.RATE_LIMITED,
                            "AI Mechanic is currently handling high volume. Please wait a moment and try again.", status, e);
                } else if (status == 400 || status == 401 || status == 403 || status == 404) {
                    throw new AiUnavailableException(ErrorCategory.CONFIG_ERROR,
                            "AI Mechanic service is temporarily misconfigured. Please try again later.", status, e);
                } else {
                    throw new AiUnavailableException(ErrorCategory.UPSTREAM_ERROR,
                            "AI Mechanic service is temporarily down. Please try again shortly.", status, e);
                }
            } catch (ResourceAccessException e) {
                long latency = System.currentTimeMillis() - attemptStart;
                log.error("[AI-{}] Gemini network/timeout failure after {}ms: {}", corrId, latency, e.getMessage());
                // For timeouts, do not escalate with repeated 15s attempts to protect client budget
                throw new AiUnavailableException(ErrorCategory.TIMEOUT,
                        "AI Mechanic took too long to respond. Please check your connection and try again.", null, e);
            } catch (Exception e) {
                long latency = System.currentTimeMillis() - attemptStart;
                log.error("[AI-{}] Gemini unexpected error after {}ms: {}", corrId, latency, e.getClass().getSimpleName());
                throw new AiUnavailableException(ErrorCategory.UPSTREAM_ERROR,
                        "AI Mechanic is temporarily busy. Please try again in a moment, or use Roadside Help / Nearby Mechanics.", null, e);
            }
        }

        throw new AiUnavailableException(ErrorCategory.UPSTREAM_ERROR,
                "AI Mechanic is temporarily busy. Please try again in a moment.", null, lastTransientError);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
