package in.raahi.backend.controller;

import com.google.firebase.auth.FirebaseToken;
import in.raahi.backend.dto.AuthDtos.VerifyRequest;
import in.raahi.backend.entity.Subscription;
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

import java.util.List;
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
        when(jwtService.issue(any(), any())).thenReturn("mock-jwt-token");
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

    @Test
    void testVerifyReconcilesExistingUserByPhoneWhenFirebaseUidDiffers() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("new-firebase-uid-999");
        when(token.getClaims()).thenReturn(Map.of("phone_number", "+919876543210"));
        when(token.getEmail()).thenReturn("existing@raahi.in");
        when(token.isEmailVerified()).thenReturn(true);
        when(firebaseVerifier.verify("valid-otp-token")).thenReturn(token);

        User existingDbUser = new User();
        existingDbUser.setId(UUID.randomUUID());
        existingDbUser.setFirebaseUid("old-firebase-uid-000");
        existingDbUser.setPhone("+919876543210");
        existingDbUser.setName("Ashish");
        existingDbUser.setVehicleType("Hatchback");
        existingDbUser.setVehicleReg("CH01AB1234");
        existingDbUser.setRole(User.Role.DRIVER);
        existingDbUser.setStatus(User.Status.ACTIVE);

        when(userRepository.findByFirebaseUid("new-firebase-uid-999")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneIn(any())).thenReturn(List.of(existingDbUser));
        when(subscriptionRepository.findByUserId(existingDbUser.getId())).thenReturn(Optional.of(mock(Subscription.class)));

        VerifyRequest req = new VerifyRequest();
        req.idToken = "valid-otp-token";

        var res = controller.verify(req, null);

        assertNotNull(res);
        assertTrue(res.success);
        assertEquals("mock-jwt-token", res.data.token);
        assertFalse(res.data.isNewUser);
        assertEquals("new-firebase-uid-999", existingDbUser.getFirebaseUid());
        assertEquals("+919876543210", existingDbUser.getPhone());
        assertEquals("existing@raahi.in", existingDbUser.getEmail());
        assertTrue(existingDbUser.isEmailVerified());

        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(userRepository, atLeastOnce()).save(existingDbUser);
    }

    @Test
    void testVerifyReconcilesExistingUserByPhoneEndingWithSuffix() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("new-firebase-uid-888");
        when(token.getClaims()).thenReturn(Map.of("phone_number", "+919876543210"));
        when(token.getEmail()).thenReturn(null);
        when(token.isEmailVerified()).thenReturn(false);
        when(firebaseVerifier.verify("token-phone-suffix")).thenReturn(token);

        User existingDbUser = new User();
        existingDbUser.setId(UUID.randomUUID());
        existingDbUser.setFirebaseUid("old-uid");
        existingDbUser.setPhone("9876543210"); // Stored without +91 in legacy db
        existingDbUser.setName("Tester");
        existingDbUser.setVehicleType("Sedan");
        existingDbUser.setVehicleReg("DL01XY9999");
        existingDbUser.setRole(User.Role.DRIVER);
        existingDbUser.setStatus(User.Status.ACTIVE);

        when(userRepository.findByFirebaseUid("new-firebase-uid-888")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneIn(any())).thenReturn(List.of(existingDbUser));
        when(subscriptionRepository.findByUserId(existingDbUser.getId())).thenReturn(Optional.of(mock(Subscription.class)));

        VerifyRequest req = new VerifyRequest();
        req.idToken = "token-phone-suffix";

        var res = controller.verify(req, null);

        assertNotNull(res);
        assertFalse(res.data.isNewUser);
        assertEquals("new-firebase-uid-888", existingDbUser.getFirebaseUid());
        assertEquals("+919876543210", existingDbUser.getPhone());
    }

    @Test
    void testVerifyAmbiguousPhoneMatchesThrowsConflict() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("ambiguous-uid");
        when(token.getClaims()).thenReturn(Map.of("phone_number", "+919876543210"));
        when(firebaseVerifier.verify("token-ambiguous")).thenReturn(token);

        User user1 = new User();
        user1.setId(UUID.randomUUID());
        User user2 = new User();
        user2.setId(UUID.randomUUID());

        when(userRepository.findByFirebaseUid("ambiguous-uid")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneIn(any())).thenReturn(List.of(user1, user2));

        VerifyRequest req = new VerifyRequest();
        req.idToken = "token-ambiguous";

        ApiException ex = assertThrows(ApiException.class, () -> controller.verify(req, null));
        assertEquals(HttpStatus.CONFLICT, ex.status);
        assertEquals("AMBIGUOUS_ACCOUNT", ex.code);
    }

    @Test
    void testVerifyBrandNewUserCreatesDriverAndInactiveSubscription() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("brand-new-uid-555");
        when(token.getClaims()).thenReturn(Map.of("phone_number", "+919999999999"));
        when(token.getEmail()).thenReturn("brandnew@raahi.in");
        when(token.isEmailVerified()).thenReturn(true);
        when(firebaseVerifier.verify("new-user-token")).thenReturn(token);

        when(userRepository.findByFirebaseUid("brand-new-uid-555")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneIn(any())).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        VerifyRequest req = new VerifyRequest();
        req.idToken = "new-user-token";

        var res = controller.verify(req, null);

        assertNotNull(res);
        assertTrue(res.data.isNewUser);
        assertEquals("+919999999999", res.data.user.phone);
        assertEquals("brandnew@raahi.in", res.data.user.email);

        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    void testVerifySuspendedUserThrowsForbidden() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("suspended-uid");
        when(firebaseVerifier.verify("suspended-token")).thenReturn(token);

        User suspendedUser = new User();
        suspendedUser.setId(UUID.randomUUID());
        suspendedUser.setFirebaseUid("suspended-uid");
        suspendedUser.setStatus(User.Status.SUSPENDED);
        when(userRepository.findByFirebaseUid("suspended-uid")).thenReturn(Optional.of(suspendedUser));

        VerifyRequest req = new VerifyRequest();
        req.idToken = "suspended-token";

        ApiException ex = assertThrows(ApiException.class, () -> controller.verify(req, null));
        assertEquals(HttpStatus.FORBIDDEN, ex.status);
        assertEquals("ACCOUNT_SUSPENDED", ex.code);
    }

    @Test
    void testVerifyForeignNumberDoesNotMatchIndianSuffixVariants() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("us-user-uid");
        when(token.getClaims()).thenReturn(Map.of("phone_number", "+19876543210"));
        when(firebaseVerifier.verify("token-us")).thenReturn(token);

        when(userRepository.findByFirebaseUid("us-user-uid")).thenReturn(Optional.empty());
        // Verify phone variants queried do not include Indian 10-digit formats
        when(userRepository.findByPhoneIn(argThat(phones ->
                !phones.contains("+919876543210") && !phones.contains("9876543210")
        ))).thenReturn(List.of());

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        VerifyRequest req = new VerifyRequest();
        req.idToken = "token-us";

        var res = controller.verify(req, null);

        assertNotNull(res);
        assertTrue(res.data.isNewUser);
        assertEquals("+19876543210", res.data.user.phone);
    }
}

