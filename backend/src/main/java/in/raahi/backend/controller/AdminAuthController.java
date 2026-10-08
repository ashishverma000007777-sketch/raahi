package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin-auth")
public class AdminAuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AdminAuthController(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        String expectedUsername = System.getenv("RAAHI_ADMIN_USERNAME");
        String expectedPassword = System.getenv("RAAHI_ADMIN_PASSWORD");
        String adminUserId = System.getenv("RAAHI_ADMIN_USER_ID");

        if (expectedUsername == null || expectedUsername.isBlank()
                || expectedPassword == null || expectedPassword.isBlank()
                || adminUserId == null || adminUserId.isBlank()) {
            throw new ApiException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "ADMIN_LOGIN_NOT_CONFIGURED",
                    "Admin login is not configured"
            );
        }

        if (!constantTimeEquals(expectedUsername, req.username)
                || !constantTimeEquals(expectedPassword, req.password)) {
            throw ApiException.forbidden("INVALID_CREDENTIALS", "Invalid admin credentials");
        }

        UUID userId;
        try {
            userId = UUID.fromString(adminUserId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "ADMIN_LOGIN_NOT_CONFIGURED",
                    "Admin user ID is invalid"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "ADMIN_USER_NOT_FOUND",
                        "Configured admin user was not found"
                ));

        if (user.getRole() != User.Role.ADMIN || user.getStatus() != User.Status.ACTIVE) {
            throw ApiException.forbidden("ADMIN_ACCESS_DISABLED", "Admin account is not active");
        }

        String token = jwtService.issue(user.getId(), User.Role.ADMIN.name());

        return ApiResponse.ok(Map.of(
                "token", token,
                "userId", user.getId().toString(),
                "name", user.getName() == null ? "Admin" : user.getName(),
                "role", user.getRole().name()
        ));
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }

    public static class LoginRequest {
        @NotBlank
        public String username;

        @NotBlank
        public String password;
    }
}
