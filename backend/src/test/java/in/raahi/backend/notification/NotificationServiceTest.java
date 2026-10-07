package in.raahi.backend.notification;

import in.raahi.backend.entity.User;
import in.raahi.backend.entity.UserNotification;
import in.raahi.backend.repository.UserNotificationRepository;
import in.raahi.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private UserNotificationRepository inboxRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        inboxRepository = mock(UserNotificationRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = new NotificationService(inboxRepository, userRepository);
    }

    @Test
    void testSendPersistsNotificationEvenWithoutFcmToken() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFcmToken(null);

        notificationService.send(user, "Test Title", "Test Body", null);

        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(inboxRepository, times(1)).save(captor.capture());
        assertEquals("Test Title", captor.getValue().getTitle());
        assertEquals("Test Body", captor.getValue().getBody());
        assertEquals(user, captor.getValue().getUser());
    }

    @Test
    void testCleanupInvalidTokenRemovesMatchingToken() {
        UUID userId = UUID.randomUUID();
        String staleToken = "dead-fcm-token-123";

        notificationService.cleanupInvalidToken(userId, staleToken);

        verify(userRepository, times(1)).clearFcmToken(userId, staleToken);
    }

    @Test
    void testCleanupInvalidTokenNoOpsOnNull() {
        notificationService.cleanupInvalidToken(null, "some-token");
        notificationService.cleanupInvalidToken(UUID.randomUUID(), null);

        verify(userRepository, never()).clearFcmToken(any(), any());
    }
}
