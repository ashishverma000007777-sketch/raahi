package in.raahi.backend.websocket;

import in.raahi.backend.entity.User;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates identity during the WebSocket HTTP upgrade handshake.
 * Primary: Reads AuthenticatedUser placed in SecurityContext by JwtAuthFilter (Bearer token header).
 * Fallback: Parses and verifies cryptographically signed JWT from ?token= or ?access_token= query parameter.
 * Rejects unauthenticated connections with HTTP 401. Never trusts raw unverified userId parameters.
 */
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthHandshakeInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 1. Try SecurityContext from Authorization header
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            attributes.put("userId", principal.userId());
            attributes.put("role", principal.role());
            return true;
        }

        // 2. Query param fallback (?token=... or ?access_token=...)
        String query = request.getURI().getQuery();
        if (query != null && !query.isBlank()) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2 && ("token".equalsIgnoreCase(pair[0]) || "access_token".equalsIgnoreCase(pair[0]))) {
                    try {
                        String rawToken = pair[1];
                        Claims claims = jwtService.parse(rawToken);
                        UUID userId = UUID.fromString(claims.getSubject());
                        Optional<User> userOpt = userRepository.findById(userId);
                        if (userOpt.isPresent() && userOpt.get().getStatus() != User.Status.SUSPENDED) {
                            attributes.put("userId", userId);
                            attributes.put("role", userOpt.get().getRole().name());
                            return true;
                        }
                    } catch (Exception ignored) {
                        // Invalid token
                    }
                }
            }
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
