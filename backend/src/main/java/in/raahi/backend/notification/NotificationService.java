package in.raahi.backend.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import in.raahi.backend.entity.User;
import in.raahi.backend.entity.UserNotification;
import in.raahi.backend.repository.UserNotificationRepository;
import in.raahi.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Handles in-app notifications and Firebase Cloud Messaging (FCM) dispatch.
 * Decoupled asynchronously so callers never block on external Google FCM latency.
 * Stale or permanently invalid tokens (UNREGISTERED, INVALID_ARGUMENT) are automatically
 * removed to prevent infinite retries and preserve only valid tokens.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final UserNotificationRepository inboxRepository;
    private final UserRepository userRepository;

    @Autowired
    public NotificationService(UserNotificationRepository inboxRepository,
                               @Autowired(required = false) UserRepository userRepository) {
        this.inboxRepository = inboxRepository;
        this.userRepository = userRepository;
    }

    public void send(User user, String title, String body, Map<String, String> data) {
        // Persist to the in-app inbox first so the unread badge reflects every notification
        // addressed to this user, even when they have no FCM token. Never throws to callers.
        try {
            UserNotification n = new UserNotification();
            n.setUser(user);
            n.setTitle(title);
            n.setBody(body);
            inboxRepository.save(n);
        } catch (Exception e) {
            log.warn("Could not persist notification for user {}: {}", user.getId(), e.getMessage());
        }
        String token = user.getFcmToken();
        if (token == null || token.isBlank()) {
            log.debug("No FCM token on file for user {}, skipping push", user.getId());
            return;
        }

        if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
            log.debug("FirebaseApp not initialized, skipping push for user {}", user.getId());
            return;
        }

        // Asynchronously dispatch the FCM push so callers never block on external Google FCM latency
        CompletableFuture.runAsync(() -> {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build());
                if (data != null) builder.putAllData(data);
                FirebaseMessaging.getInstance().send(builder.build());
            } catch (FirebaseMessagingException e) {
                MessagingErrorCode errorCode = e.getMessagingErrorCode();
                // Clean up stale, unregistered or invalid tokens so we don't retry dead tokens forever
                if (errorCode == MessagingErrorCode.UNREGISTERED
                        || errorCode == MessagingErrorCode.INVALID_ARGUMENT
                        || (e.getErrorCode() != null && "NOT_FOUND".equalsIgnoreCase(e.getErrorCode().name()))) {
                    log.info("Cleaning up permanently invalid FCM token for user {}: {}", user.getId(), errorCode);
                    cleanupInvalidToken(user.getId(), token);
                } else {
                    log.warn("FCM send failed for user {}: {}", user.getId(), e.getMessage());
                }
            } catch (Exception e) {
                log.warn("Unexpected error sending FCM to user {}: {}", user.getId(), e.getMessage());
            }
        });
    }

    void cleanupInvalidToken(java.util.UUID userId, String token) {
        if (userRepository != null && userId != null && token != null) {
            try {
                userRepository.clearFcmToken(userId, token);
            } catch (Exception ex) {
                log.warn("Could not clear invalid FCM token for user {}: {}", userId, ex.getMessage());
            }
        }
    }
}
