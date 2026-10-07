package in.raahi.backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry that supports multiple active sessions per user and prevents
 * session wiping during reconnection flapping.
 *
 * Every session is wrapped in ConcurrentWebSocketSessionDecorator (5s send timeout, 64KB buffer)
 * so concurrent writes from background threads never trigger TEXT_PARTIAL_WRITING IllegalStateException.
 *
 * Supports distributed multi-instance deployment via Redis pub/sub when Redis is available,
 * and seamlessly degrades to local in-memory operation in standalone mode or unit tests.
 */
@Component
public class WebSocketSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionRegistry.class);
    public static final String WS_CHANNEL = "raahi:ws:events";

    private final String instanceId = UUID.randomUUID().toString();
    private final Map<UUID, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StringRedisTemplate redisTemplate;
    private RedisMessageListenerContainer listenerContainer;

    @Autowired
    public WebSocketSessionRegistry(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void initRedisPubSub() {
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            try {
                RedisMessageListenerContainer container = new RedisMessageListenerContainer();
                container.setConnectionFactory(redisTemplate.getConnectionFactory());
                container.addMessageListener(new MessageListener() {
                    @Override
                    public void onMessage(Message message, byte[] pattern) {
                        try {
                            String body = new String(message.getBody(), StandardCharsets.UTF_8);
                            JsonNode node = objectMapper.readTree(body);
                            String originInstance = node.path("instanceId").asText("");
                            if (instanceId.equals(originInstance)) {
                                return; // Skip echo of message sent by this instance
                            }
                            String targetUserStr = node.path("userId").asText(null);
                            String payload = node.path("payload").asText(null);
                            if (targetUserStr != null && payload != null) {
                                deliverLocal(UUID.fromString(targetUserStr), new TextMessage(payload));
                            }
                        } catch (Exception ex) {
                            log.warn("Failed to process Redis pub/sub WebSocket message: {}", ex.getMessage());
                        }
                    }
                }, new ChannelTopic(WS_CHANNEL));
                container.afterPropertiesSet();
                container.start();
                this.listenerContainer = container;
                log.info("Initialized distributed WebSocket pub/sub on Redis channel '{}'", WS_CHANNEL);
            } catch (Exception e) {
                log.warn("Could not initialize Redis WebSocket listener (running local-only): {}", e.getMessage());
            }
        }
    }

    @PreDestroy
    public void stopRedisPubSub() {
        if (listenerContainer != null) {
            try {
                listenerContainer.stop();
            } catch (Exception ignored) {}
        }
    }

    public void register(UUID userId, WebSocketSession rawSession) {
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(rawSession, 5000, 64 * 1024);
        sessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(rawSession.getId(), decorated);
        log.debug("Registered WebSocket session {} for user {}", rawSession.getId(), userId);
    }

    public void unregister(UUID userId, WebSocketSession session) {
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session.getId());
            if (userSessions.isEmpty()) {
                sessions.remove(userId, userSessions);
            }
            log.debug("Unregistered WebSocket session {} for user {}", session.getId(), userId);
        }
    }

    public void unregister(UUID userId) {
        sessions.remove(userId);
    }

    public void closeSessions(UUID userId) {
        Map<String, WebSocketSession> userSessions = sessions.remove(userId);
        if (userSessions != null) {
            for (WebSocketSession s : userSessions.values()) {
                try {
                    s.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public boolean sendToUser(UUID userId, String event, Object data) {
        String payload;
        try {
            Map<String, Object> map = Map.of("event", event, "data", data);
            payload = objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialize WebSocket event payload: {}", e.getMessage());
            return false;
        }

        TextMessage textMessage = new TextMessage(payload);
        boolean deliveredLocally = deliverLocal(userId, textMessage);

        // Publish to Redis channel so other cluster instances can deliver to their connected sessions
        if (redisTemplate != null) {
            try {
                Map<String, String> redisEnvelope = Map.of(
                        "instanceId", instanceId,
                        "userId", userId.toString(),
                        "payload", payload
                );
                redisTemplate.convertAndSend(WS_CHANNEL, objectMapper.writeValueAsString(redisEnvelope));
            } catch (Exception e) {
                log.warn("Failed to publish WebSocket message to Redis channel: {}", e.getMessage());
            }
        }

        return deliveredLocally;
    }

    public boolean deliverLocal(UUID userId, TextMessage textMessage) {
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return false;
        }

        boolean anySent = false;
        for (WebSocketSession session : userSessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                    anySent = true;
                } catch (IOException e) {
                    log.warn("Failed to send WebSocket message on session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
        return anySent;
    }
}
