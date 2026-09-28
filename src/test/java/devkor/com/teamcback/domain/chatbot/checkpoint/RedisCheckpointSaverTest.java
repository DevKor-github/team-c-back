package devkor.com.teamcback.domain.chatbot.checkpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisCheckpointSaverTest {
    @Test
    void persistsLoadsRefreshesTtlAndReleasesNamespacedCheckpoint() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);

        AtomicReference<String> stored = new AtomicReference<>();
        when(values.get(anyString())).thenAnswer(invocation -> stored.get());
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), eq(60L), eq(TimeUnit.MINUTES));
        ChatbotProperties properties = new ChatbotProperties(true,
                new ChatbotProperties.Llm("test", "test", 100, 5),
                new ChatbotProperties.Agent(6, 5, 60, 10),
                new ChatbotProperties.Tools(new ChatbotProperties.Limits(5, 10),
                        new ChatbotProperties.Limits(10, 20), 7,
                        new ChatbotProperties.Limits(5, 10)),
                new ChatbotProperties.RateLimit(30, 10, 5, "Asia/Seoul"));
        RedisCheckpointSaver saver = new RedisCheckpointSaver(redis, new ObjectMapper(), properties);
        RunnableConfig config = RunnableConfig.builder()
                .threadId("menu:" + UUID.randomUUID())
                .build();
        Checkpoint checkpoint = Checkpoint.builder()
                .id("checkpoint-1")
                .state(Map.of("owner", "anonymous", "awaiting", "SELECT"))
                .nodeId("ask")
                .nextNodeId("select")
                .build();

        config = saver.put(config, checkpoint);
        verify(values).set(eq("chatbot:checkpoint:" + config.threadId().orElseThrow()),
                anyString(), eq(60L), eq(TimeUnit.MINUTES));

        Checkpoint loaded = saver.get(config).orElseThrow();
        assertThat(loaded.getId()).isEqualTo("checkpoint-1");
        assertThat(loaded.getState()).containsEntry("owner", "anonymous");
        assertThat(loaded.getNodeId()).isEqualTo("ask");
        assertThat(loaded.getNextNodeId()).isEqualTo("select");
        verify(redis).expire(eq("chatbot:checkpoint:" + config.threadId().orElseThrow()),
                eq(60L), eq(TimeUnit.MINUTES));

        saver.release(config);
        verify(redis).delete("chatbot:checkpoint:" + config.threadId().orElseThrow());
    }
}
