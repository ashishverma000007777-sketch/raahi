package in.raahi.backend.service;

import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

/** Admin authorization is decided from the database row (role + status), never from a client-held claim alone. */
@Component
public class AdminGuard {

    private final UserRepository userRepository;

    public AdminGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User require(AuthenticatedUser principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.forbidden("FORBIDDEN", "Admin access required"));
        if (user.getRole() != User.Role.ADMIN || user.getStatus() != User.Status.ACTIVE) {
            throw ApiException.forbidden("FORBIDDEN", "Admin access required");
        }
        return user;
    }
}
