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

    public static final String DEFAULT_MODEL = "gemini-1.5-flash";
    public static final String DEFAULT_API_VERSION = "v1beta";
    public static final String DEFAULT_ENDPOINT = "https://generativelanguage.googleapis.com";

    public static final String FRIENDLY_UNAVAILABLE_MESSAGE =
            "AI Mechanic is temporarily busy. Please try again in a moment, or use Roadside Help / Nearby Mechanics.";

    private static final int MAX_ATTEMPTS = 2;
    private static final long INITIAL_BACKOFF_MS = 600;

    private final String apiKey;
    private final String model;
    private final String apiVersion;
    private final String endpoint;
    private final RestTemplate restTemplate;

    public GeminiAiMechanicProvider(
            @Value("${raahi.ai.gemini-api-key:}") String apiKey,
            @Value("${raahi.ai.gemini-model:" + DEFAULT_MODEL + "}") String model,
            @Value("${raahi.ai.gemini-api-version:" + DEFAULT_API_VERSION + "}") String apiVersion,
            @Value("${raahi.ai.gemini-endpoint:" + DEFAULT_ENDPOINT + "}") String endpoint
    ) {
        this.apiKey = apiKey;
        this.model = sanitizeModel(model);
        this.apiVersion = sanitizeApiVersion(apiVersion);
        this.endpoint = sanitizeEndpoint(endpoint);
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(20000);
        this.restTemplate = new RestTemplate(factory);
    }

    public GeminiAiMechanicProvider(
            @Value("${raahi.ai.gemini-api-key:}") String apiKey,
            @Value("${raahi.ai.gemini-model:" + DEFAULT_MODEL + "}") String model
    ) {
        this(apiKey, model, DEFAULT_API_VERSION, DEFAULT_ENDPOINT);
    }

    public GeminiAiMechanicProvider(String apiKey, String model, String apiVersion, String endpoint, RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.model = sanitizeModel(model);
        this.apiVersion = sanitizeApiVersion(apiVersion);
        this.endpoint = sanitizeEndpoint(endpoint);
        this.restTemplate = restTemplate;
    }

    public static String sanitizeModel(String rawModel) {
        if (rawModel == null || rawModel.isBlank()) {
            return DEFAULT_MODEL;
        }
        String clean = rawModel.trim();
        if (clean.startsWith("models/")) {
            clean = clean.substring("models/".length());
        }
        // If an invalid or non-existent model (such as "skimaki-agy-cheap" or placeholder) is provided,
        // log a warning and fall back to the verified, supported model.
        if ("skimaki-agy-cheap".equalsIgnoreCase(clean) || clean.toLowerCase().contains("skimaki")) {
            log.warn("Invalid Gemini model '{}' detected. Automatically falling back to verified model '{}'",
                    rawModel, DEFAULT_MODEL);
            return DEFAULT_MODEL;
        }
        return clean;
    }

    public static String sanitizeApiVersion(String rawApiVersion) {
        if (rawApiVersion == null || rawApiVersion.isBlank()) {
            return DEFAULT_API_VERSION;
        }
        String clean = rawApiVersion.trim().replaceAll("^/+|/+$", "");
        // If an invalid API version like "v1main" is encountered, fall back to "v1beta"
        if ("v1main".equalsIgnoreCase(clean) || clean.toLowerCase().contains("main")) {
            log.warn("Invalid Gemini API version '{}' detected. Falling back to '{}'", rawApiVersion, DEFAULT_API_VERSION);
            return DEFAULT_API_VERSION;
        }
        return clean;
    }

    public static String sanitizeEndpoint(String rawEndpoint) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return DEFAULT_ENDPOINT;
        }
        return rawEndpoint.trim().replaceAll("/+$", "");
    }

    public String getModel() { return model; }
    public String getApiVersion() { return apiVersion; }
    public String getEndpoint() { return endpoint; }

    @Override
    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public String providerName() { return "gemini"; }

    @Override
    public String complete(List<ChatTurn> history) throws AiUnavailableException {
        String corrId = java.util.UUID.randomUUID().toString().substring(0, 8);
        long startMs = System.currentTimeMillis();

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[AI-{}] Gemini API key is missing or blank.", corrId);
            throw new AiUnavailableException(ErrorCategory.CONFIG_ERROR,
                    "AI Mechanic service is temporarily misconfigured. Please try again later.", null, null);
        }

        String url = endpoint + "/" + apiVersion + "/models/" + model + ":generateContent";

        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatTurn turn : history) {
            String geminiRole = "assistant".equals(turn.role()) ? "model" : "user";
            contents.add(Map.of("role", geminiRole, "parts", List.of(Map.of("text", turn.content()))));
        }
        if (contents.isEmpty()) {
            contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", "Hello"))));
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

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long attemptStart = System.currentTimeMillis();
            try {
                log.info("[AI-{}] Sending request to Gemini (attempt {}/{}, model {}, turns {})",
                        corrId, attempt, MAX_ATTEMPTS, model, history.size());
                JsonNode response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), JsonNode.class);
                long latency = System.currentTimeMillis() - attemptStart;
                if (response == null) {
                    log.error("[AI-{}] Gemini response body is null after {}ms", corrId, latency);
                    throw new AiUnavailableException(ErrorCategory.MALFORMED_RESPONSE,
                            "AI Mechanic returned an empty response. Please try again.", null, null);
                }
                JsonNode candidates = response.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode parts = firstCandidate.path("content").path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (JsonNode part : parts) {
                            if (part.hasNonNull("text")) {
                                sb.append(part.get("text").asText());
                            }
                        }
                        String reply = sb.toString().trim();
                        if (!reply.isBlank()) {
                            log.info("[AI-{}] Gemini responded successfully in {}ms (total elapsed: {}ms)",
                                    corrId, latency, System.currentTimeMillis() - startMs);
                            return reply;
                        }
                    }
                    String finishReason = firstCandidate.path("finishReason").asText("");
                    if ("SAFETY".equalsIgnoreCase(finishReason)) {
                        log.warn("[AI-{}] Gemini response blocked by safety filters", corrId);
                        return "For safety reasons, please consult a certified mechanic or use Raahi's Roadside Help / SOS feature.";
                    }
                }
                log.error("[AI-{}] Gemini text missing or empty in response after {}ms", corrId, latency);
                throw new AiUnavailableException(ErrorCategory.MALFORMED_RESPONSE,
                        "AI Mechanic response could not be parsed. Please try again.", null, null);
            } catch (AiUnavailableException e) {
                throw e;
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                long latency = System.currentTimeMillis() - attemptStart;
                boolean isTransient = (status == 429 || status == 503 || status == 504);
                if (isTransient && attempt < MAX_ATTEMPTS) {
                    log.warn("[AI-{}] Gemini attempt {}/{} failed with transient HTTP status {} in {}ms. Retrying in {}ms...",
                            corrId, attempt, MAX_ATTEMPTS, status, latency, backoffMs);
                    lastTransientError = e;
                    sleepQuietly(backoffMs);
                    backoffMs *= 2;
                    continue;
                }
                log.error("[AI-{}] Gemini request failed: HTTP {} after {}ms", corrId, status, latency);
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
