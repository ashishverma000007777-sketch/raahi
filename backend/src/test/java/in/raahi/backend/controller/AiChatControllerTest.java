package in.raahi.backend.controller;

import in.raahi.backend.ai.AiMechanicProvider;
import in.raahi.backend.dto.AiChatDtos.SendMessageRequest;
import in.raahi.backend.entity.AiChatMessage;
import in.raahi.backend.entity.AiChatSession;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.AiChatMessageRepository;
import in.raahi.backend.repository.AiChatSessionRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiChatControllerTest {

    private AiChatSessionRepository sessionRepository;
    private AiChatMessageRepository messageRepository;
    private UserRepository userRepository;
    private AiMechanicProvider aiMechanicProvider;
    private RateLimiter rateLimiter;
    private AiChatController controller;

    private AuthenticatedUser principal;
    private UUID userId;
    private UUID sessionId;
    private User testUser;
    private AiChatSession session;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(AiChatSessionRepository.class);
        messageRepository = mock(AiChatMessageRepository.class);
        userRepository = mock(UserRepository.class);
        aiMechanicProvider = mock(AiMechanicProvider.class);
        rateLimiter = mock(RateLimiter.class);

        controller = new AiChatController(sessionRepository, messageRepository, userRepository, aiMechanicProvider, rateLimiter);

        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        principal = new AuthenticatedUser(userId, "DRIVER");

        testUser = new User();
        testUser.setId(userId);

        session = new AiChatSession();
        session.setId(sessionId);
        session.setUser(testUser);

        when(rateLimiter.allow(anyString(), anyInt(), any())).thenReturn(true);
        when(sessionRepository.findByIdWithUser(sessionId)).thenReturn(Optional.of(session));
    }

    @Test
    void testBlankMessageRejected() {
        SendMessageRequest req = new SendMessageRequest();
        req.content = "   ";

        ApiException ex = assertThrows(ApiException.class, () -> controller.sendMessage(principal, sessionId, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
        assertEquals("EMPTY_MESSAGE", ex.code);
    }

    @Test
    void testTooLongMessageRejected() {
        SendMessageRequest req = new SendMessageRequest();
        req.content = "a".repeat(2001);

        ApiException ex = assertThrows(ApiException.class, () -> controller.sendMessage(principal, sessionId, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
        assertEquals("MESSAGE_TOO_LONG", ex.code);
    }

    @Test
    void testRpmRateLimitEnforced() {
        when(rateLimiter.allow(startsWith("ai:rpm:"), eq(AiChatController.MAX_RPM), any())).thenReturn(false);

        SendMessageRequest req = new SendMessageRequest();
        req.content = "Check engine light is flashing";

        ApiException ex = assertThrows(ApiException.class, () -> controller.sendMessage(principal, sessionId, req));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.status);
        assertEquals("AI_RATE_LIMIT", ex.code);
    }

    @Test
    void testDailyQuotaRateLimitEnforced() {
        when(rateLimiter.allow(startsWith("ai:rpm:"), anyInt(), any())).thenReturn(true);
        when(rateLimiter.allow(startsWith("ai:daily:"), eq(AiChatController.MAX_DAILY_QUOTA), any())).thenReturn(false);

        SendMessageRequest req = new SendMessageRequest();
        req.content = "Check engine light is flashing";

        ApiException ex = assertThrows(ApiException.class, () -> controller.sendMessage(principal, sessionId, req));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.status);
        assertEquals("AI_DAILY_QUOTA_EXCEEDED", ex.code);
    }

    @Test
    void testUnownedSessionReturnsNotFound() {
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(otherUserId);

        AiChatSession otherSession = new AiChatSession();
        otherSession.setId(sessionId);
        otherSession.setUser(otherUser);

        when(sessionRepository.findByIdWithUser(sessionId)).thenReturn(Optional.of(otherSession));

        SendMessageRequest req = new SendMessageRequest();
        req.content = "Help with my tire";

        ApiException ex = assertThrows(ApiException.class, () -> controller.sendMessage(principal, sessionId, req));
        assertEquals(HttpStatus.NOT_FOUND, ex.status);
        assertEquals("SESSION_NOT_FOUND", ex.code);
    }

    @Test
    void testMaxSessionMessagesEnforced() {
        when(messageRepository.countBySessionId(sessionId)).thenReturn(100L);

        SendMessageRequest req = new SendMessageRequest();
        req.content = "Another message";

        ApiException ex = assertThrows(ApiException.class, () -> controller.sendMessage(principal, sessionId, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
        assertEquals("SESSION_LIMIT_EXCEEDED", ex.code);
    }

    @Test
    void testMaxSessionsPerUserEnforced() {
        when(sessionRepository.countByUserId(userId)).thenReturn(50L);

        ApiException ex = assertThrows(ApiException.class, () -> controller.createSession(principal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
        assertEquals("MAX_SESSIONS_REACHED", ex.code);
    }
}
