package in.raahi.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMillis;

    public static final int MIN_SECRET_LENGTH_BYTES = 32; // 256 bits

    private static final java.util.Set<String> DISALLOWED_SECRETS = java.util.Set.of(
            "raahi-dev-secret-change-in-production",
            "your-super-secret-jwt-key-change-this-in-production",
            "12345678901234567890123456789012",
            "abcdefghijklmnopqrstuvwxyz123456",
            "secretsecretsecretsecretsecret32"
    );

    public JwtService(
            @Value("${raahi.jwt.secret:}") String secret,
            @Value("${raahi.jwt.expiry-days:30}") long expiryDays) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not configured or is blank. Refusing to start.");
        }
        if (!secret.equals(secret.trim())) {
            throw new IllegalStateException("JWT_SECRET must not contain leading or trailing whitespace (padded secrets are strictly forbidden).");
        }
        byte[] secretBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short. Minimum required length is " + MIN_SECRET_LENGTH_BYTES +
                    " bytes (256 bits) for HMAC-SHA256, but received " + secretBytes.length + " bytes. Never use weak or padded secrets.");
        }
        for (String disallowed : DISALLOWED_SECRETS) {
            if (secret.toLowerCase(java.util.Locale.ROOT).contains(disallowed)) {
                throw new IllegalStateException("JWT_SECRET uses an insecure default or well-known placeholder. Please generate a strong cryptographically random secret.");
            }
        }
        if (secret.chars().distinct().count() < 8) {
            throw new IllegalStateException("JWT_SECRET has insufficient entropy (too repetitive or padded with repeating characters).");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiryMillis = Duration.ofDays(expiryDays).toMillis();
    }

    public String issue(UUID userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMillis))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
