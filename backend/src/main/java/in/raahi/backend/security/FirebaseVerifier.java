package in.raahi.backend.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import in.raahi.backend.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class FirebaseVerifier {

    private static final Logger log = LoggerFactory.getLogger(FirebaseVerifier.class);

    private final String credentialsPath;
    private final boolean devAuthEnabled;
    private volatile boolean initialized = false;

    @Autowired
    public FirebaseVerifier(
            @Value("${raahi.firebase.credentials-path:}") String credentialsPath,
            @Value("${raahi.auth.dev-mock-enabled:false}") boolean devAuthEnabled,
            @Autowired(required = false) Environment environment) {
        this.credentialsPath = credentialsPath;
        // In production, dev authentication is strictly disabled regardless of configuration flags
        boolean isProdProfile = environment != null && (environment.matchesProfiles("prod") || environment.matchesProfiles("production"));
        String envNode = System.getenv("NODE_ENV");
        String envSpring = System.getenv("SPRING_PROFILES_ACTIVE");
        String envRaahi = System.getenv("RAAHI_ENV");
        boolean isProdEnv = "production".equalsIgnoreCase(envNode)
                || (envSpring != null && (envSpring.contains("prod") || envSpring.contains("production")))
                || "production".equalsIgnoreCase(envRaahi);
        boolean isProd = isProdProfile || isProdEnv;
        this.devAuthEnabled = devAuthEnabled && !isProd;
    }

    public FirebaseVerifier(String credentialsPath, boolean devAuthEnabled) {
        this(credentialsPath, devAuthEnabled, null);
    }

    @PostConstruct
    public void init() {
        if (FirebaseApp.getApps().isEmpty()) {
            if (credentialsPath == null || credentialsPath.isBlank()) {
                log.warn("FIREBASE_CREDENTIALS_PATH is not set. Service account JSON missing; running in unconfigured Firebase mode.");
                return;
            }
            File file = new File(credentialsPath);
            if (!file.exists()) {
                log.warn("FIREBASE_CREDENTIALS_PATH points to non-existent file '{}'. Running in unconfigured Firebase mode.", credentialsPath);
                return;
            }
            try (FileInputStream serviceAccount = new FileInputStream(file)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                initialized = true;
                log.info("FirebaseApp initialized successfully from {}", credentialsPath);
            } catch (Exception e) {
                log.error("Failed to initialize FirebaseApp from {}: {}", credentialsPath, e.getMessage());
            }
        } else {
            initialized = true;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isDevAuthEnabled() {
        return devAuthEnabled;
    }

    /** Verifies the Firebase ID token and returns the decoded token, or throws 401/503. */
    public FirebaseToken verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token is required");
        }

        if (idToken.startsWith("dev-mock:")) {
            if (!devAuthEnabled) {
                log.warn("Rejected dev-mock token: mock authentication is disabled in this environment.");
                throw new ApiException(HttpStatus.UNAUTHORIZED, "DEV_AUTH_DISABLED",
                        "Mock authentication is disabled in this environment.");
            }
            // Support dev testing only when explicitly enabled: "dev-mock:<uid>:<phone>"
            String[] parts = idToken.split(":", 3);
            String uid = parts.length > 1 && !parts[1].isBlank() ? parts[1] : "dev-user-123";
            String phone = parts.length > 2 && !parts[2].isBlank() ? parts[2] : "+919999999999";
            try {
                var ctor = FirebaseToken.class.getDeclaredConstructor(Map.class);
                ctor.setAccessible(true);
                Map<String, Object> claims = new HashMap<>();
                claims.put("user_id", uid);
                claims.put("sub", uid);
                claims.put("phone_number", phone);
                claims.put("email", uid + "@raahi.dev");
                return ctor.newInstance(claims);
            } catch (Exception e) {
                log.error("Could not create dev mock token: {}", e.getMessage());
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DEV_AUTH_ERROR", "Failed to parse dev mock token");
            }
        }

        if (!initialized) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "FIREBASE_UNCONFIGURED",
                    "Firebase authentication is not configured on this server (FIREBASE_CREDENTIALS_PATH missing or invalid).");
        }

        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken, true);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Firebase token invalid or expired");
        }
    }
}
