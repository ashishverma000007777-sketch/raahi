package in.raahi.backend.security;

import com.google.firebase.auth.FirebaseToken;
import in.raahi.backend.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FirebaseVerifierTest {

    @Test
    void testDevMockWorksInDevelopmentWhenExplicitlyEnabled() {
        FirebaseVerifier verifier = new FirebaseVerifier("", true);
        assertTrue(verifier.isDevAuthEnabled());

        FirebaseToken token = verifier.verify("dev-mock:my-uid:+919876543210");
        assertNotNull(token);
        assertEquals("my-uid", token.getUid());
        assertEquals("+919876543210", token.getClaims().get("phone_number"));
    }

    @Test
    void testDevMockRejectedWhenDisabledByDefault() {
        FirebaseVerifier verifier = new FirebaseVerifier("", false);
        assertFalse(verifier.isDevAuthEnabled());

        ApiException ex = assertThrows(ApiException.class, () -> verifier.verify("dev-mock:attacker:+919876543210"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status);
        assertEquals("DEV_AUTH_DISABLED", ex.code);
    }

    @Test
    void testDevMockRejectedInProductionProfileEvenIfFlagIsTrue() {
        Environment env = Mockito.mock(Environment.class);
        when(env.matchesProfiles("prod")).thenReturn(true);

        FirebaseVerifier verifier = new FirebaseVerifier("", true, env);
        assertFalse(verifier.isDevAuthEnabled(), "Production profile must force devAuthEnabled to false");

        ApiException ex = assertThrows(ApiException.class, () -> verifier.verify("dev-mock:attacker:+919876543210"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status);
        assertEquals("DEV_AUTH_DISABLED", ex.code);
    }

    @Test
    void testDevMockRejectedInProductionNamedProfile() {
        Environment env = Mockito.mock(Environment.class);
        when(env.matchesProfiles("production")).thenReturn(true);

        FirebaseVerifier verifier = new FirebaseVerifier("", true, env);
        assertFalse(verifier.isDevAuthEnabled(), "Production named profile must force devAuthEnabled to false");

        ApiException ex = assertThrows(ApiException.class, () -> verifier.verify("dev-mock:attacker:+919876543210"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status);
        assertEquals("DEV_AUTH_DISABLED", ex.code);
    }

    @Test
    void testInvalidOrNullTokenRejected() {
        FirebaseVerifier verifier = new FirebaseVerifier("", false);

        ApiException exNull = assertThrows(ApiException.class, () -> verifier.verify(null));
        assertEquals(HttpStatus.UNAUTHORIZED, exNull.status);
        assertEquals("INVALID_TOKEN", exNull.code);

        ApiException exBlank = assertThrows(ApiException.class, () -> verifier.verify("   "));
        assertEquals(HttpStatus.UNAUTHORIZED, exBlank.status);
        assertEquals("INVALID_TOKEN", exBlank.code);
    }

    @Test
    void testRealTokenRejectedWhenFirebaseUnconfigured() {
        FirebaseVerifier verifier = new FirebaseVerifier("", false);
        assertFalse(verifier.isInitialized());

        ApiException ex = assertThrows(ApiException.class, () -> verifier.verify("some-real-firebase-jwt-token"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status);
        assertEquals("FIREBASE_UNCONFIGURED", ex.code);
    }
}
