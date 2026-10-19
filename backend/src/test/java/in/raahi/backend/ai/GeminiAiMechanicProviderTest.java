package in.raahi.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GeminiAiMechanicProviderTest {

    private RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
    }

    @Test
    void testSanitizeModelReplacesInvalidSkimakiModel() {
        assertEquals("gemini-1.5-flash", GeminiAiMechanicProvider.sanitizeModel("skimaki-agy-cheap"));
        assertEquals("gemini-1.5-flash", GeminiAiMechanicProvider.sanitizeModel("SKIMAKI-AGY-CHEAP"));
        assertEquals("gemini-1.5-flash", GeminiAiMechanicProvider.sanitizeModel("models/skimaki-agy-cheap"));
        assertEquals("gemini-1.5-flash", GeminiAiMechanicProvider.sanitizeModel(null));
        assertEquals("gemini-1.5-flash", GeminiAiMechanicProvider.sanitizeModel("   "));
    }

    @Test
    void testSanitizeModelStripsModelsPrefix() {
        assertEquals("gemini-1.5-flash", GeminiAiMechanicProvider.sanitizeModel("models/gemini-1.5-flash"));
        assertEquals("gemini-2.0-flash", GeminiAiMechanicProvider.sanitizeModel("models/gemini-2.0-flash"));
        assertEquals("gemini-1.5-pro", GeminiAiMechanicProvider.sanitizeModel("gemini-1.5-pro"));
    }

    @Test
    void testSanitizeApiVersionReplacesV1Main() {
        assertEquals("v1beta", GeminiAiMechanicProvider.sanitizeApiVersion("v1main"));
        assertEquals("v1beta", GeminiAiMechanicProvider.sanitizeApiVersion("/v1main/"));
        assertEquals("v1beta", GeminiAiMechanicProvider.sanitizeApiVersion(null));
        assertEquals("v1", GeminiAiMechanicProvider.sanitizeApiVersion("v1"));
        assertEquals("v1beta", GeminiAiMechanicProvider.sanitizeApiVersion("/v1beta/"));
    }

    @Test
    void testDefaultValuesAndSanitizationInConstructor() {
        GeminiAiMechanicProvider provider = new GeminiAiMechanicProvider(
                "test-api-key",
                "skimaki-agy-cheap",
                "v1main",
                "https://generativelanguage.googleapis.com/",
                restTemplate
        );

        assertEquals("gemini-1.5-flash", provider.getModel());
        assertEquals("v1beta", provider.getApiVersion());
        assertEquals("https://generativelanguage.googleapis.com", provider.getEndpoint());
        assertTrue(provider.isConfigured());
    }

    @Test
    void testMissingApiKeyThrowsConfigError() {
        GeminiAiMechanicProvider provider = new GeminiAiMechanicProvider(
                "",
                "gemini-1.5-flash",
                "v1beta",
                "https://generativelanguage.googleapis.com",
                restTemplate
        );

        assertFalse(provider.isConfigured());

        AiMechanicProvider.AiUnavailableException ex = assertThrows(
                AiMechanicProvider.AiUnavailableException.class,
                () -> provider.complete(List.of(new AiMechanicProvider.ChatTurn("user", "Hello")))
        );

        assertEquals(AiMechanicProvider.ErrorCategory.CONFIG_ERROR, ex.getCategory());
        assertTrue(ex.getMessage().contains("misconfigured"));
    }

    @Test
    void testSuccessfulCompletion() throws Exception {
        GeminiAiMechanicProvider provider = new GeminiAiMechanicProvider(
                "valid-key",
                "gemini-1.5-flash",
                "v1beta",
                "https://generativelanguage.googleapis.com",
                restTemplate
        );

        String json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {"text": "Check tyre pressure and inspect for punctures."}
                    ]
                  }
                }
              ]
            }
            """;
        JsonNode mockResponse = mapper.readTree(json);

        when(restTemplate.postForObject(
                eq("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(mockResponse);

        String reply = provider.complete(List.of(new AiMechanicProvider.ChatTurn("user", "Flat tyre help")));
        assertEquals("Check tyre pressure and inspect for punctures.", reply);
    }

    @Test
    void testSafetyBlockReturnsAdvisory() throws Exception {
        GeminiAiMechanicProvider provider = new GeminiAiMechanicProvider(
                "valid-key",
                "gemini-1.5-flash",
                "v1beta",
                "https://generativelanguage.googleapis.com",
                restTemplate
        );

        String json = """
            {
              "candidates": [
                {
                  "finishReason": "SAFETY",
                  "content": {
                    "parts": []
                  }
                }
              ]
            }
            """;
        JsonNode mockResponse = mapper.readTree(json);

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(mockResponse);

        String reply = provider.complete(List.of(new AiMechanicProvider.ChatTurn("user", "How to bypass brakes")));
        assertTrue(reply.contains("safety reasons"));
    }

    @Test
    void testHttp404ThrowsConfigError() {
        GeminiAiMechanicProvider provider = new GeminiAiMechanicProvider(
                "valid-key",
                "gemini-1.5-flash",
                "v1beta",
                "https://generativelanguage.googleapis.com",
                restTemplate
        );

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Model not found"));

        AiMechanicProvider.AiUnavailableException ex = assertThrows(
                AiMechanicProvider.AiUnavailableException.class,
                () -> provider.complete(List.of(new AiMechanicProvider.ChatTurn("user", "Engine light")))
        );

        assertEquals(AiMechanicProvider.ErrorCategory.CONFIG_ERROR, ex.getCategory());
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void testHttp429ThrowsRateLimited() {
        GeminiAiMechanicProvider provider = new GeminiAiMechanicProvider(
                "valid-key",
                "gemini-1.5-flash",
                "v1beta",
                "https://generativelanguage.googleapis.com",
                restTemplate
        );

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Quota exceeded"));

        AiMechanicProvider.AiUnavailableException ex = assertThrows(
                AiMechanicProvider.AiUnavailableException.class,
                () -> provider.complete(List.of(new AiMechanicProvider.ChatTurn("user", "Battery dead")))
        );

        assertEquals(AiMechanicProvider.ErrorCategory.RATE_LIMITED, ex.getCategory());
        assertEquals(429, ex.getHttpStatus());
    }

    @Test
    void testResourceAccessExceptionThrowsTimeout() {
        GeminiAiMechanicProvider provider = new GeminiAiMechanicProvider(
                "valid-key",
                "gemini-1.5-flash",
                "v1beta",
                "https://generativelanguage.googleapis.com",
                restTemplate
        );

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        AiMechanicProvider.AiUnavailableException ex = assertThrows(
                AiMechanicProvider.AiUnavailableException.class,
                () -> provider.complete(List.of(new AiMechanicProvider.ChatTurn("user", "Overheating")))
        );

        assertEquals(AiMechanicProvider.ErrorCategory.TIMEOUT, ex.getCategory());
    }

    @Test
    void testSpringContextBeanInstantiationSuccess() {
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext context =
                     new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.register(GeminiAiMechanicProvider.class);
            context.refresh();
            GeminiAiMechanicProvider bean = context.getBean(GeminiAiMechanicProvider.class);
            assertNotNull(bean, "GeminiAiMechanicProvider must be cleanly instantiated by Spring container");
        }
    }
}
