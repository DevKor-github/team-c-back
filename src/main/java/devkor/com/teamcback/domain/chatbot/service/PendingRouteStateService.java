package devkor.com.teamcback.domain.chatbot.service;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_SESSION_FORBIDDEN;
import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.dto.PendingRouteState;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Redis-backed state for an incomplete NAVIGATE_ROUTE interaction. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class PendingRouteStateService {
    private static final String KEY_PREFIX = "chatbot:pending:route:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatbotProperties properties;

    public Optional<PendingRouteState> load(UUID sessionId, ChatCaller caller) {
        try {
            String value = redisTemplate.opsForValue().get(key(sessionId));
            if (value == null) {
                return Optional.empty();
            }
            StoredPendingRoute stored = objectMapper.readValue(value, StoredPendingRoute.class);
            if (!stored.owner().equals(caller.key())) {
                throw new GlobalException(CHATBOT_SESSION_FORBIDDEN);
            }
            redisTemplate.expire(key(sessionId), properties.agent().pendingRouteTtlMinutes(), TimeUnit.MINUTES);
            return Optional.ofNullable(stored.state());
        } catch (GlobalException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        }
    }

    public void save(UUID sessionId, ChatCaller caller, PendingRouteState state) {
        try {
            String value = objectMapper.writeValueAsString(new StoredPendingRoute(caller.key(), state));
            redisTemplate.opsForValue().set(key(sessionId), value,
                    properties.agent().pendingRouteTtlMinutes(), TimeUnit.MINUTES);
        } catch (JsonProcessingException exception) {
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        }
    }

    public void delete(UUID sessionId, ChatCaller caller) {
        load(sessionId, caller);
        redisTemplate.delete(key(sessionId));
    }

    private String key(UUID sessionId) {
        return KEY_PREFIX + sessionId;
    }

    record StoredPendingRoute(String owner, PendingRouteState state) {
    }
}
