package in.raahi.backend.controller;

import com.google.firebase.auth.FirebaseToken;
import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.AuthDtos.*;
import in.raahi.backend.entity.Subscription;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.SubscriptionRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.FirebaseVerifier;
import in.raahi.backend.security.JwtService;
import in.raahi.backend.security.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final FirebaseVerifier firebaseVerifier;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final JwtService jwtService;
    private final in.raahi.backend.websocket.WebSocketSessionRegistry sessionRegistry;
    private final RateLimiter rateLimiter;

    public AuthController(FirebaseVerifier firebaseVerifier, UserRepository userRepository,
                           SubscriptionRepository subscriptionRepository, JwtService jwtService,
                           in.raahi.backend.websocket.WebSocketSessionRegistry sessionRegistry,
                           RateLimiter rateLimiter) {
        this.firebaseVerifier = firebaseVerifier;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.jwtService = jwtService;
        this.sessionRegistry = sessionRegistry;
        this.rateLimiter = rateLimiter;
    }

    // Replaces the old /auth/verifyOtp + /auth/verify-firebase duplicate endpoints.
    @PostMapping("/verify")
    public ApiResponse<VerifyResponse> verify(@Valid @RequestBody VerifyRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
        String clientIp = httpReq != null ? httpReq.getHeader("X-Forwarded-For") : null;
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = httpReq != null ? httpReq.getRemoteAddr() : "unknown";
        } else if (clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        String ipKey = "auth:verify:ip:" + clientIp;
        if (!rateLimiter.allow(ipKey, 15, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many verification attempts from this IP. Please wait.");
        }

        String verifyKey = "auth:verify:" + (req.idToken.length() > 32 ? req.idToken.substring(0, 32) : req.idToken);
        if (!rateLimiter.allow(verifyKey, 10, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many verification attempts. Please wait.");
        }

        FirebaseToken decoded = firebaseVerifier.verify(req.idToken);

        boolean isNewUser = userRepository.findByFirebaseUid(decoded.getUid()).isEmpty();

        User user = userRepository.findByFirebaseUid(decoded.getUid()).orElseGet(() -> {
            User u = new User();
            u.setFirebaseUid(decoded.getUid());
            u.setPhone(decoded.getClaims().get("phone_number") != null
                    ? decoded.getClaims().get("phone_number").toString() : null);
            u.setEmail(decoded.getEmail());
            // SECURITY: client can only ever request DRIVER at signup. MECHANIC/HELPER require
            // a separate application+approval flow; ADMIN is never client-selectable, period.
            u.setRole(User.Role.DRIVER);
            User saved = userRepository.save(u);

            // Mirrors the old Node signup logic: every new DRIVER gets a NONE/INACTIVE
            // subscription row so the 402 gate has something to check against. Mechanics/
            // Helpers don't need one — they're never gated by requireSubscription.
            Subscription sub = new Subscription();
            sub.setUser(saved);
            sub.setTier(Subscription.Tier.NONE);
            sub.setStatus(Subscription.Status.INACTIVE);
            subscriptionRepository.save(sub);

            return saved;
        });

        if (user.getStatus() == User.Status.SUSPENDED) {
            throw ApiException.forbidden("ACCOUNT_SUSPENDED", "Account suspended");
        }

        String token = jwtService.issue(user.getId(), user.getRole().name());

        VerifyResponse res = new VerifyResponse();
        res.token = token;
        res.isNewUser = isNewUser;
        res.user = toDto(user);
        return ApiResponse.ok(res);
    }

    /**
     * DEV-ONLY admin bootstrap.
     * Requires RAAHI_ADMIN_BOOTSTRAP_SECRET and is intended only for local/staging testing.
     */
    @PostMapping("/dev/bootstrap-admin")
    public ApiResponse<UserDto> bootstrapAdmin(
            @RequestHeader(value = "X-Admin-Bootstrap-Secret", required = false) String secret,
            @RequestParam String userId) {

        String devAuth = System.getenv("RAAHI_DEV_AUTH_ENABLED");
        if (!"true".equalsIgnoreCase(devAuth)) {
            throw ApiException.notFound("NOT_FOUND", "Not found");
        }

        String expected = System.getenv("RAAHI_ADMIN_BOOTSTRAP_SECRET");
        if (expected == null || expected.isBlank()
                || secret == null || !java.security.MessageDigest.isEqual(
                    expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw ApiException.forbidden("FORBIDDEN", "Invalid bootstrap secret");
        }

        java.util.UUID targetId;
        try {
            targetId = java.util.UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_USER_ID", "Invalid userId");
        }

        User user = userRepository.findById(targetId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        user.setRole(User.Role.ADMIN);
        user.setStatus(User.Status.ACTIVE);
        userRepository.save(user);

        return ApiResponse.ok(toDto(user));
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        return ApiResponse.ok(toDto(user));
    }

    @PutMapping("/profile")
    public ApiResponse<UserDto> updateProfile(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @RequestBody UpdateProfileRequest req) {
        if (!rateLimiter.allow("auth:profile:" + principal.userId(), 20, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Profile updates too frequent. Please wait.");
        }

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        if (req.name != null) {
            String clean = req.name.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
            if (clean.length() > 100) throw ApiException.badRequest("NAME_TOO_LONG", "Name exceeds 100 characters");
            user.setName(clean);
        }
        if (req.vehicleType != null) {
            String clean = req.vehicleType.replaceAll("[\\p{Cntrl}]", "").trim();
            if (clean.length() > 50) throw ApiException.badRequest("TYPE_TOO_LONG", "Vehicle type exceeds 50 characters");
            user.setVehicleType(clean);
        }
        if (req.vehicleReg != null) {
            String clean = req.vehicleReg.trim().toUpperCase();
            if (clean.length() > 20 || (!clean.isEmpty() && !clean.matches("^[A-Z0-9 -]{4,20}$"))) {
                throw ApiException.badRequest("INVALID_REG", "Invalid vehicle registration format");
            }
            user.setVehicleReg(clean);
        }
        if (req.language != null) {
            String clean = req.language.trim().toLowerCase();
            if (clean.length() > 10) throw ApiException.badRequest("INVALID_LANGUAGE", "Invalid language code");
            user.setLanguage(clean);
        }

        userRepository.save(user);
        return ApiResponse.ok(toDto(user));
    }

    @PutMapping("/location")
    public ApiResponse<Object> updateLocation(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @RequestBody LocationRequest req) {
        if (!rateLimiter.allow("auth:location:" + principal.userId(), 60, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Location updates too frequent");
        }

        if (req.lat == null || req.lng == null) {
            throw ApiException.badRequest("MISSING_COORDS", "lat and lng required");
        }
        if (req.lat < -90 || req.lat > 90 || req.lng < -180 || req.lng > 180) {
            throw ApiException.badRequest("INVALID_COORDS", "lat/lng out of range");
        }

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        user.setLastLat(req.lat);
        user.setLastLng(req.lng);
        user.setLastLocationAt(Instant.now());
        userRepository.save(user);
        return ApiResponse.ok(java.util.Map.of("updated", true));
    }

    @PutMapping("/fcm-token")
    public ApiResponse<Object> updateFcmToken(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @RequestBody FcmTokenRequest req) {
        if (!rateLimiter.allow("auth:fcm:" + principal.userId(), 10, Duration.ofHours(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "FCM token updates too frequent");
        }

        if (req.token == null || req.token.isBlank()) {
            throw ApiException.badRequest("MISSING_TOKEN", "token is required");
        }
        if (req.token.length() > 500) {
            throw ApiException.badRequest("TOKEN_TOO_LONG", "FCM token exceeds 500 characters");
        }

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        user.setFcmToken(req.token);
        userRepository.save(user);
        return ApiResponse.ok(java.util.Map.of("updated", true));
    }

    @PostMapping("/logout")
    public ApiResponse<Object> logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal != null) {
            userRepository.findById(principal.userId()).ifPresent(user -> {
                user.setFcmToken(null);
                userRepository.save(user);
            });
            sessionRegistry.closeSessions(principal.userId());
        }
        return ApiResponse.ok(java.util.Map.of("loggedOut", true));
    }

    private UserDto toDto(User u) {
        UserDto dto = new UserDto();
        dto.id = u.getId().toString();
        dto.name = u.getName();
        dto.phone = u.getPhone();
        dto.role = u.getRole().name();
        dto.vehicleType = u.getVehicleType();
        dto.vehicleReg = u.getVehicleReg();
        dto.isVerified = u.isVerified();
        dto.ratingAvg = u.getRatingAvg() == null ? 0 : u.getRatingAvg();
        dto.totalHelps = u.getTotalHelps() == null ? 0 : u.getTotalHelps();
        return dto;
    }
}
