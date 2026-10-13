package devkor.com.teamcback.domain.chatbot.service;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_RATE_LIMITED;
import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_SESSION_FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class ChatbotOperationsTest {
    private final ChatbotProperties properties = new ChatbotProperties(true,
            new ChatbotProperties.Llm("google", "model", 500, 8),
            new ChatbotProperties.Agent(6, 5, 60, 15),
            new ChatbotProperties.Tools(new ChatbotProperties.Limits(5, 10),
                    new ChatbotProperties.Limits(10, 20), 7, new ChatbotProperties.Limits(5, 10)),
            new ChatbotProperties.RateLimit(30, 10, 5, "Asia/Seoul"));

    @Test
    void sessionMemoryKeepsFiveTurnsRefreshesTtlAndExcludesCoordinates() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        AtomicReference<String> stored = new AtomicReference<>();
        when(values.get(anyString())).thenAnswer(invocation -> stored.get());
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), any(Long.class), any(java.util.concurrent.TimeUnit.class));

        ObjectMapper objectMapper = new ObjectMapper();
        ChatSessionMemoryService memory = new ChatSessionMemoryService(redis, objectMapper, properties);
        java.util.UUID sessionId = java.util.UUID.randomUUID();
        ChatCaller caller = ChatCaller.from(null, "127.0.0.1");
        for (int index = 1; index <= 6; index++) {
            memory.save(sessionId, caller, "message-" + index, "reply-" + index);
        }

        JsonNode document = objectMapper.readTree(stored.get());
        assertThat(document.at("/turns")).hasSize(5);
        assertThat(document.toString()).contains("message-2", "reply-6")
                .doesNotContain("37.5861", "127.0290", "currentLocation");
        assertThat(memory.load(sessionId, caller)).hasSize(5);
        org.mockito.Mockito.verify(redis, org.mockito.Mockito.atLeastOnce())
                .expire(anyString(), org.mockito.ArgumentMatchers.eq(60L),
                        org.mockito.ArgumentMatchers.eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    void sessionMemoryRejectsDifferentOwner() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ObjectMapper objectMapper = new ObjectMapper();
        when(values.get(anyString())).thenReturn(objectMapper.writeValueAsString(
                new ChatSessionMemoryService.StoredSession("user:1", java.util.List.of())));
        ChatSessionMemoryService memory = new ChatSessionMemoryService(redis, objectMapper, properties);

        assertThatThrownBy(() -> memory.load(java.util.UUID.randomUUID(), ChatCaller.from(null, "127.0.0.1")))
                .isInstanceOfSatisfying(GlobalException.class,
                        exception -> assertThat(exception.getResultCode()).isEqualTo(CHATBOT_SESSION_FORBIDDEN));
    }

    @Test
    void rateLimiterRejectsBeforeDailyLimitCanBeExceeded() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenReturn(1L, 31L);
        ChatRateLimiter limiter = new ChatRateLimiter(redis, properties);

        assertThatThrownBy(() -> limiter.check(ChatCaller.from(null, "127.0.0.1")))
                .isInstanceOfSatisfying(GlobalException.class,
                        exception -> assertThat(exception.getResultCode()).isEqualTo(CHATBOT_RATE_LIMITED));
    }

    @Test
    void toolCallLimiterBlocksSeventhInvocation() {
        ChatbotToolCallLimiter limiter = new ChatbotToolCallLimiter(properties);
        try (ChatbotToolCallLimiter.Scope scope = limiter.open()) {
            for (int index = 0; index < 6; index++) {
                scope.beforeToolCall();
            }
            assertThat(scope.callCount()).isEqualTo(6);
            assertThatThrownBy(scope::beforeToolCall).isInstanceOf(ToolCallLimitExceededException.class);
        }
    }
}
