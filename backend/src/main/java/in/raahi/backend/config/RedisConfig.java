package in.raahi.backend.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.URI;
import java.time.Duration;

@Configuration
public class RedisConfig {
    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${REDIS_URL:${UPSTASH_REDIS_URL:${REDIS_TLS_URL:}}}")
    private String redisUrl;

    @Value("${spring.data.redis.host:${REDIS_HOST:localhost}}")
    private String host;

    @Value("${spring.data.redis.port:${REDIS_PORT:6379}}")
    private int port;

    @Value("${spring.data.redis.password:${REDIS_PASSWORD:}}")
    private String password;

    @Value("${spring.data.redis.ssl.enabled:${REDIS_SSL_ENABLED:false}}")
    private boolean sslEnabled;

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        boolean useSsl = sslEnabled;

        if (redisUrl != null && !redisUrl.isBlank()) {
            try {
                URI uri = URI.create(redisUrl.trim());
                serverConfig.setHostName(uri.getHost());
                serverConfig.setPort(uri.getPort() > 0 ? uri.getPort() : 6379);

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    String pass = userInfo.length > 1 ? userInfo[1] : userInfo[0];
                    if (userInfo.length > 1 && !userInfo[0].isBlank() && !"default".equalsIgnoreCase(userInfo[0])) {
                        serverConfig.setUsername(userInfo[0]);
                    }
                    serverConfig.setPassword(RedisPassword.of(pass));
                }

                if ("rediss".equalsIgnoreCase(uri.getScheme()) ||
                    (uri.getHost() != null && uri.getHost().toLowerCase().contains("upstash.io"))) {
                    useSsl = true;
                }
                log.info("Configured Redis connection from URL to host: {}, port: {}, ssl: {}",
                        serverConfig.getHostName(), serverConfig.getPort(), useSsl);
            } catch (Exception e) {
                log.error("Failed to parse Redis URL '{}': {}. Falling back to host/port configuration.", redisUrl, e.getMessage());
                serverConfig.setHostName(host);
                serverConfig.setPort(port);
                if (password != null && !password.isBlank()) {
                    serverConfig.setPassword(RedisPassword.of(password));
                }
            }
        } else {
            serverConfig.setHostName(host);
            serverConfig.setPort(port);
            if (password != null && !password.isBlank()) {
                serverConfig.setPassword(RedisPassword.of(password));
            }
            if (host != null && host.toLowerCase().contains("upstash.io")) {
                useSsl = true;
            }
            log.info("Configured Redis connection to host: {}, port: {}, ssl: {}", host, port, useSsl);
        }

        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .build();

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
                LettuceClientConfiguration.builder()
                        .clientOptions(clientOptions)
                        .commandTimeout(Duration.ofSeconds(5));

        if (useSsl) {
            clientConfigBuilder.useSsl();
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfigBuilder.build());
        factory.setValidateConnection(false); // Do not crash application startup if Redis is temporarily unreachable
        return factory;
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
