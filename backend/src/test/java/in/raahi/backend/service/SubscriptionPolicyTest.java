package in.raahi.backend.service;

import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubscriptionPolicyTest {

    private User driver() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setRole(User.Role.DRIVER);
        return u;
    }

    @Test
    void mvpDefaultAllowsDriverWithoutSubscription() {
        SubscriptionRepository repo = mock(SubscriptionRepository.class);
        assertDoesNotThrow(() -> new SubscriptionPolicy(repo, false).requireForJobCreation(driver()));
        verifyNoInteractions(repo);
    }

    @Test
    void enforcedModeStillReturns402WithoutActiveSubscription() {
        SubscriptionRepository repo = mock(SubscriptionRepository.class);
        when(repo.findByUserId(any())).thenReturn(Optional.empty());
        ApiException e = assertThrows(ApiException.class,
                () -> new SubscriptionPolicy(repo, true).requireForJobCreation(driver()));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, e.status);
    }
}
