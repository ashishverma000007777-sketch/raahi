package in.raahi.backend.ai;

import in.raahi.backend.ai.AiMechanicProvider.ChatTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeminiAiMechanicProviderTest {

    private GeminiAiMechanicProvider provider;

    @BeforeEach
    void setUp() {
        provider = new GeminiAiMechanicProvider("test-key", "gemini-2.0-flash");
    }

    @Test
    void testBuildGeminiContentsAlternatingRoles() {
        List<ChatTurn> history = List.of(
                new ChatTurn("user", "Engine clicking noise"),
                new ChatTurn("user", "Also headlights are dim"),
                new ChatTurn("assistant", "Sounds like a weak battery or alternator issue."),
                new ChatTurn("user", "Can I jump start it?")
        );

        List<Map<String, Object>> contents = provider.buildGeminiContents(history);

        assertEquals(3, contents.size(), "Consecutive user turns must be merged into one turn");
        assertEquals("user", contents.get(0).get("role"));
        assertTrue(((List<Map<String, String>>) contents.get(0).get("parts")).get(0).get("text")
                .contains("Engine clicking noise\n\nAlso headlights are dim"));

        assertEquals("model", contents.get(1).get("role"));
        assertEquals("user", contents.get(2).get("role"));
    }

    @Test
    void testBuildGeminiContentsLeadingModelTurnStripped() {
        List<ChatTurn> history = List.of(
                new ChatTurn("assistant", "Earlier response after history truncation"),
                new ChatTurn("user", "Temperature gauge in red"),
                new ChatTurn("assistant", "Pull over safely.")
        );

        List<Map<String, Object>> contents = provider.buildGeminiContents(history);

        assertFalse(contents.isEmpty());
        assertEquals("user", contents.get(0).get("role"), "Gemini API requires conversation to start with user turn");
        assertEquals(2, contents.size());
    }

    @Test
    void testBuildGeminiContentsIgnoresNullOrBlank() {
        List<ChatTurn> history = List.of(
                new ChatTurn("user", "   "),
                new ChatTurn("user", null),
                new ChatTurn("user", "Valid question")
        );

        List<Map<String, Object>> contents = provider.buildGeminiContents(history);

        assertEquals(1, contents.size());
        assertEquals("user", contents.get(0).get("role"));
        assertEquals("Valid question", ((List<Map<String, String>>) contents.get(0).get("parts")).get(0).get("text"));
    }

    @Test
    void testCompleteMissingApiKeyThrowsConfigError() {
        GeminiAiMechanicProvider unconfigured = new GeminiAiMechanicProvider("", "gemini-2.0-flash");
        assertFalse(unconfigured.isConfigured());

        AiMechanicProvider.AiUnavailableException ex = assertThrows(
                AiMechanicProvider.AiUnavailableException.class,
                () -> unconfigured.complete(List.of(new ChatTurn("user", "hello")))
        );
        assertEquals(AiMechanicProvider.ErrorCategory.CONFIG_ERROR, ex.getCategory());
    }

    @Test
    void testCompleteEmptyHistoryThrowsMalformed() {
        AiMechanicProvider.AiUnavailableException ex = assertThrows(
                AiMechanicProvider.AiUnavailableException.class,
                () -> provider.complete(List.of(new ChatTurn("user", "   ")))
        );
        assertEquals(AiMechanicProvider.ErrorCategory.MALFORMED_RESPONSE, ex.getCategory());
    }
}
