package devkor.com.teamcback.domain.chatbot.checkpoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.InterruptionMetadata;
import org.bsc.langgraph4j.checkpoint.AbstractCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis-backed LangGraph4j saver for compact, JSON-safe workflow state. */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class RedisCheckpointSaver extends AbstractCheckpointSaver {
    private static final String KEY_PREFIX = "chatbot:checkpoint:";
    private static final TypeReference<List<StoredCheckpoint>> TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatbotProperties properties;

    public RedisCheckpointSaver(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                ChatbotProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    protected LinkedList<Checkpoint> loadCheckpoints(RunnableConfig config) throws Exception {
        String value = redisTemplate.opsForValue().get(key(config));
        if (value == null || value.isBlank()) {
            return new LinkedList<>();
        }
        refresh(config);
        LinkedList<Checkpoint> checkpoints = new LinkedList<>();
        for (StoredCheckpoint stored : objectMapper.readValue(value, TYPE)) {
            checkpoints.add(Checkpoint.builder()
                    .id(stored.id())
                    .state(stored.state() == null ? Map.of() : stored.state())
                    .nodeId(stored.nodeId())
                    .nextNodeId(stored.nextNodeId())
                    .build());
        }
        return checkpoints;
    }

    @Override
    protected void insertedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints,
                                      Checkpoint checkpoint) throws Exception {
        persist(config, checkpoints);
    }

    @Override
    protected void updatedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints,
                                     Checkpoint checkpoint) throws Exception {
        persist(config, checkpoints);
    }

    @Override
    protected BaseCheckpointSaver.Tag releaseCheckpoints(RunnableConfig config,
                                                         LinkedList<Checkpoint> checkpoints,
                                                         String checkpointId) {
        redisTemplate.delete(key(config));
        return new BaseCheckpointSaver.Tag(threadId(config), checkpoints);
    }

    @Override
    protected BaseCheckpointSaver.Tag releaseCheckpointsOnError(RunnableConfig config,
                                                                LinkedList<Checkpoint> checkpoints,
                                                                Throwable throwable) {
        redisTemplate.delete(key(config));
        return new BaseCheckpointSaver.Tag(threadId(config), checkpoints);
    }

    @Override
    public <State extends org.bsc.langgraph4j.state.AgentState>
    CompletableFuture<InterruptionMetadata<State>> registerInterruption(
            RunnableConfig config, InterruptionMetadata<State> interruption) {
        return CompletableFuture.completedFuture(interruption);
    }

    @Override
    public java.util.Optional<BaseCheckpointSaver.Tag> tag(RunnableConfig config, Integer version)
            throws Exception {
        return java.util.Optional.of(new BaseCheckpointSaver.Tag(threadId(config), version, List.of()));
    }

    private void persist(RunnableConfig config, Collection<Checkpoint> checkpoints) throws Exception {
        List<StoredCheckpoint> stored = checkpoints.stream()
                .map(checkpoint -> new StoredCheckpoint(checkpoint.getId(), checkpoint.getState(),
                        checkpoint.getNodeId(), checkpoint.getNextNodeId()))
                .toList();
        redisTemplate.opsForValue().set(key(config), objectMapper.writeValueAsString(stored),
                properties.agent().sessionTtlMinutes(), TimeUnit.MINUTES);
    }

    private void refresh(RunnableConfig config) {
        redisTemplate.expire(key(config), properties.agent().sessionTtlMinutes(), TimeUnit.MINUTES);
    }

    private String key(RunnableConfig config) {
        return KEY_PREFIX + config.threadId().orElseThrow();
    }

    record StoredCheckpoint(String id, Map<String, Object> state, String nodeId, String nextNodeId) {}
}
