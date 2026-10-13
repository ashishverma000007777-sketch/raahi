package in.raahi.backend.controller;

import com.google.firebase.auth.FirebaseToken;
import in.raahi.backend.dto.AuthDtos.VerifyRequest;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.HelperApplicationRepository;
import in.raahi.backend.repository.SubscriptionRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.FirebaseVerifier;
import in.raahi.backend.security.JwtService;
import in.raahi.backend.security.RateLimiter;
import in.raahi.backend.websocket.WebSocketSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private FirebaseVerifier firebaseVerifier;
    private UserRepository userRepository;
    private SubscriptionRepository subscriptionRepository;
    private HelperApplicationRepository helperApplicationRepository;
    private JwtService jwtService;
    private WebSocketSessionRegistry sessionRegistry;
    private RateLimiter rateLimiter;
    private AuthController controller;

    private UUID userId;
    private User user;
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        firebaseVerifier = mock(FirebaseVerifier.class);
        userRepository = mock(UserRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        helperApplicationRepository = mock(HelperApplicationRepository.class);
        jwtService = mock(JwtService.class);
        sessionRegistry = mock(WebSocketSessionRegistry.class);
        rateLimiter = mock(RateLimiter.class);

        when(rateLimiter.allow(anyString(), anyInt(), any())).thenReturn(true);

        controller = new AuthController(
                firebaseVerifier,
                userRepository,
                subscriptionRepository,
                helperApplicationRepository,
                jwtService,
                sessionRegistry,
                rateLimiter
        );

        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setFirebaseUid("firebase-uid-123");
        user.setRole(User.Role.DRIVER);
        user.setStatus(User.Status.ACTIVE);

        principal = new AuthenticatedUser(userId, "DRIVER");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testSyncEmailVerifiedWhenTokenMatches() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("firebase-uid-123");
        when(token.getEmail()).thenReturn("helper@raahi.in");
        when(token.isEmailVerified()).thenReturn(true);
        when(firebaseVerifier.verify("valid-id-token")).thenReturn(token);

        VerifyRequest req = new VerifyRequest();
        req.idToken = "valid-id-token";
        req.email = "helper@raahi.in";

        var res = controller.syncEmail(principal, req);

        assertTrue(res.data.emailVerified);
        assertEquals("helper@raahi.in", res.data.email);
        assertTrue(user.isEmailVerified());
        assertEquals("helper@raahi.in", user.getEmail());
    }

    @Test
    void testSyncEmailUnverifiedWhenTokenEmailNotVerifiedYet() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("firebase-uid-123");
        when(token.getEmail()).thenReturn("helper@raahi.in");
        when(token.isEmailVerified()).thenReturn(false);
        when(firebaseVerifier.verify("token-pending")).thenReturn(token);

        VerifyRequest req = new VerifyRequest();
        req.idToken = "token-pending";
        req.email = "helper@raahi.in";

        var res = controller.syncEmail(principal, req);

        assertFalse(res.data.emailVerified);
        assertFalse(user.isEmailVerified());
        assertEquals("helper@raahi.in", res.data.email);
    }

    @Test
    void testSyncEmailUnverifiedWhenRequestedEmailDoesNotMatchToken() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("firebase-uid-123");
        when(token.getEmail()).thenReturn("old@raahi.in");
        when(token.isEmailVerified()).thenReturn(true);
        when(firebaseVerifier.verify("token-old-email")).thenReturn(token);

        VerifyRequest req = new VerifyRequest();
        req.idToken = "token-old-email";
        req.email = "new@raahi.in"; // Client wants new email, but token is still for old email

        var res = controller.syncEmail(principal, req);

        assertFalse(res.data.emailVerified);
        assertFalse(user.isEmailVerified());
        assertEquals("new@raahi.in", res.data.email);
    }

    @Test
    void testSyncEmailRejectsUidMismatch() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("different-uid");
        when(firebaseVerifier.verify("stranger-token")).thenReturn(token);

        VerifyRequest req = new VerifyRequest();
        req.idToken = "stranger-token";
        req.email = "helper@raahi.in";

        ApiException ex = assertThrows(ApiException.class, () -> controller.syncEmail(principal, req));
        assertEquals(HttpStatus.FORBIDDEN, ex.status);
        assertEquals("UID_MISMATCH", ex.code);
    }

    @Test
    void testSyncEmailRejectsInvalidFirebaseToken_Http401() {
        when(firebaseVerifier.verify("invalid-token")).thenThrow(
                new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Firebase token invalid or expired")
        );

        VerifyRequest req = new VerifyRequest();
        req.idToken = "invalid-token";
        req.email = "helper@raahi.in";

        ApiException ex = assertThrows(ApiException.class, () -> controller.syncEmail(principal, req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status);
        assertEquals("INVALID_TOKEN", ex.code);
    }

    @Test
    void testSyncEmailHandlesFirebaseUnconfigured_Http503() {
        when(firebaseVerifier.verify("any-token")).thenThrow(
                new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "FIREBASE_UNCONFIGURED", "Firebase not configured")
        );

        VerifyRequest req = new VerifyRequest();
        req.idToken = "any-token";
        req.email = "helper@raahi.in";

        ApiException ex = assertThrows(ApiException.class, () -> controller.syncEmail(principal, req));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status);
        assertEquals("FIREBASE_UNCONFIGURED", ex.code);
    }

    @Test
    void testSyncEmailRateLimited_Http429() {
        when(rateLimiter.allow(startsWith("auth:sync-email:"), anyInt(), any())).thenReturn(false);

        VerifyRequest req = new VerifyRequest();
        req.idToken = "some-token";
        req.email = "helper@raahi.in";

        ApiException ex = assertThrows(ApiException.class, () -> controller.syncEmail(principal, req));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.status);
        assertEquals("RATE_LIMIT_EXCEEDED", ex.code);
    }

    @Test
    void testSyncEmailFailsOnDatabaseError_ThrowsDataAccessException() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("firebase-uid-123");
        when(token.getEmail()).thenReturn("helper@raahi.in");
        when(token.isEmailVerified()).thenReturn(true);
        when(firebaseVerifier.verify("token-db-fail")).thenReturn(token);

        when(userRepository.save(any(User.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("Simulated database failure on save")
        );

        VerifyRequest req = new VerifyRequest();
        req.idToken = "token-db-fail";
        req.email = "helper@raahi.in";

        assertThrows(org.springframework.dao.DataAccessException.class, () -> controller.syncEmail(principal, req));
    }
}

