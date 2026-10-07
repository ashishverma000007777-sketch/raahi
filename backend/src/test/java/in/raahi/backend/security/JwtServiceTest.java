package in.raahi.backend.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "test-secret-key-that-is-at-least-thirty-two-bytes-long-123456";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, 30);
    }

    @Test
    void testIssueAndParseToken() {
        UUID userId = UUID.randomUUID();
        String role = "DRIVER";

        String token = jwtService.issue(userId, role);
        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = jwtService.parse(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("DRIVER", claims.get("role"));
        assertNotNull(claims.getExpiration());
    }

    @Test
    void testRefusesEmptyOrNullSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", 30));
        assertThrows(IllegalStateException.class, () -> new JwtService("   ", 30));
        assertThrows(IllegalStateException.class, () -> new JwtService(null, 30));
    }

    @Test
    void testRefusesWeakOrShortSecret() {
        // Less than 32 bytes (256 bits) must be rejected without padding
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JwtService("too-short-secret", 30));
        assertTrue(ex.getMessage().contains("JWT_SECRET is too short"));
    }

    @Test
    void testValidSecretAcceptance() {
        assertDoesNotThrow(() -> new JwtService("this-is-a-valid-256-bit-key-with-over-thirty-two-bytes!", 30));
    }

    @Test
    void testRefusesPaddedSecretWithWhitespace() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService("   short-secret-with-padding   ", 30));
        assertTrue(ex.getMessage().contains("leading or trailing whitespace"));
    }

    @Test
    void testRefusesLowEntropySecret() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 30));
        assertTrue(ex.getMessage().contains("insufficient entropy"));
    }

    @Test
    void testRefusesInsecureDefaultSecret() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService("raahi-dev-secret-change-in-production-123456", 30));
        assertTrue(ex.getMessage().contains("insecure default"));
    }

    @Test
    void testTokenExpirationDateIsInFuture() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.issue(userId, "DRIVER");
        Claims claims = jwtService.parse(token);
        assertTrue(claims.getExpiration().after(new java.util.Date()));
    }
}
