package devkor.com.teamcback.domain.chatbot.service;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_SESSION_FORBIDDEN;
import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class ChatSessionMemoryService {
    private static final String KEY_PREFIX = "chatbot:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatbotProperties properties;

    public List<ChatTurn> load(UUID sessionId, ChatCaller caller) {
        try {
            String value = redisTemplate.opsForValue().get(key(sessionId));
            if (value == null) {
                return List.of();
            }
            StoredSession session = objectMapper.readValue(value, StoredSession.class);
            if (!session.owner().equals(caller.key())) {
                throw new GlobalException(CHATBOT_SESSION_FORBIDDEN);
            }
            redisTemplate.expire(key(sessionId), properties.agent().sessionTtlMinutes(), TimeUnit.MINUTES);
            return List.copyOf(session.turns());
        } catch (GlobalException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        }
    }

    public void save(UUID sessionId, ChatCaller caller, String userMessage, String assistantReply) {
        List<ChatTurn> turns = new ArrayList<>(load(sessionId, caller));
        turns.add(new ChatTurn(userMessage, assistantReply));
        int historyTurns = properties.agent().historyTurns();
        if (turns.size() > historyTurns) {
            turns = new ArrayList<>(turns.subList(turns.size() - historyTurns, turns.size()));
        }
        try {
            String value = objectMapper.writeValueAsString(new StoredSession(caller.key(), turns));
            redisTemplate.opsForValue().set(key(sessionId), value,
                    properties.agent().sessionTtlMinutes(), TimeUnit.MINUTES);
        } catch (JsonProcessingException exception) {
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        }
    }

    private String key(UUID sessionId) {
        return KEY_PREFIX + sessionId;
    }

    record StoredSession(String owner, List<ChatTurn> turns) {
    }

    public record ChatTurn(String userMessage, String assistantReply) {
    }
}
