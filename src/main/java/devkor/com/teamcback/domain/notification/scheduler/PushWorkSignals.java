package devkor.com.teamcback.domain.notification.scheduler;

import devkor.com.teamcback.domain.notification.entity.PushMessage;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Redis contains wake-up hints only; Postgres remains the durable source of work. */
@Component
public class PushWorkSignals {
    public enum Work { DISPATCH, RECEIPT, SURVEY }

    private static final Logger log = LoggerFactory.getLogger(PushWorkSignals.class);
    private static final DefaultRedisScript<Long> SIGNAL = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if not current or tonumber(ARGV[1]) < tonumber(current) then
                redis.call('SET', KEYS[1], ARGV[1])
            end
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> CLAIM = new DefaultRedisScript<>("""
            local due = redis.call('GET', KEYS[1])
            if not due or tonumber(due) > tonumber(ARGV[1]) then return 0 end
            if not redis.call('SET', KEYS[2], ARGV[2], 'NX', 'PX', 300000) then return 0 end
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final boolean neon;
    private final String namespace;
    private final String audience;
    private final Map<Work, Long> localDue = new ConcurrentHashMap<>();
    private final Map<Work, Long> redisFailureProbeAt = new ConcurrentHashMap<>();

    public PushWorkSignals(RedisConnectionFactory factory, Clock clock,
            @Value("${push.storage:mysql}") String storage,
            @Value("${push.idle.namespace:kodaero-push}") String namespace,
            @Value("${push.audience:INTERNAL_TEST}") String audience) {
        // Do not share RedisUtil's mutable value serializer.
        this.redis = new StringRedisTemplate(factory);
        this.clock = clock;
        this.neon = "neon".equals(storage);
        this.namespace = namespace;
        this.audience = audience;
    }

    String key(Work work) {
        return "push:wake:v1:" + namespace + ":" + work
                + (work == Work.SURVEY ? ":" + audience : "");
    }

    public void wakeAfterCommit(Work work, LocalDateTime due) {
        if (!neon || due == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { wake(work, due); }
            });
        } else {
            wake(work, due);
        }
    }

    public void messagesChangedAfterCommit(Collection<PushMessage> messages) {
        Map<Work, LocalDateTime> next = new EnumMap<>(Work.class);
        for (PushMessage message : messages) {
            switch (message.getStatus()) {
                case QUEUED -> next.merge(Work.DISPATCH,
                        message.getNextRetryAt() == null ? LocalDateTime.now(clock) : message.getNextRetryAt(),
                        (a, b) -> a.isBefore(b) ? a : b);
                case RECEIPT_PENDING -> {
                    if (message.getReceiptAvailableAt() != null) next.merge(Work.RECEIPT,
                            message.getReceiptAvailableAt(), (a, b) -> a.isBefore(b) ? a : b);
                }
                default -> { }
            }
        }
        next.forEach(this::wakeAfterCommit);
    }

    public void wake(Work work, LocalDateTime due) {
        if (!neon || due == null) return;
        long timestamp = due.atZone(clock.getZone()).toInstant().toEpochMilli();
        try {
            redis.execute(SIGNAL, List.of(key(work)), Long.toString(timestamp));
        } catch (RuntimeException error) {
            localDue.merge(work, timestamp, Math::min);
            log.warn("Push wake hint deferred to local fallback. work={}, errorType={}",
                    work, error.getClass().getSimpleName());
        }
    }

    /** null means idle; token is retained until completion to coordinate all backend instances. */
    public String claim(Work work) {
        if (!neon) return "legacy";
        long now = clock.millis();
        String token = UUID.randomUUID().toString();
        try {
            // Replay hints recorded during a previous Redis outage before claiming.
            Long local = localDue.remove(work);
            if (local != null) {
                try { redis.execute(SIGNAL, List.of(key(work)), Long.toString(local)); }
                catch (RuntimeException error) { localDue.merge(work, local, Math::min); throw error; }
            }
            return Long.valueOf(1).equals(redis.execute(CLAIM,
                    List.of(key(work), key(work) + ":lock"), Long.toString(now), token)) ? token : null;
        } catch (RuntimeException error) {
            // Redis failure must not restore 5-second Postgres polling. Reconcile at most hourly.
            return claimLocal(work, now);
        }
    }

    private synchronized String claimLocal(Work work, long now) {
        Long hint = localDue.get(work);
        if (hint != null && hint <= now && localDue.remove(work, hint)) {
            redisFailureProbeAt.put(work, now + 3_600_000);
            return "local";
        }
        long nextProbe = redisFailureProbeAt.getOrDefault(work, 0L);
        if (nextProbe > now) return null;
        redisFailureProbeAt.put(work, now + 3_600_000);
        return "local";
    }

    public void release(Work work, String token) {
        if (!neon || "local".equals(token)) return;
        try { redis.execute(RELEASE, List.of(key(work) + ":lock"), token); }
        catch (RuntimeException error) {
            log.warn("Push wake lease will expire. work={}, errorType={}", work, error.getClass().getSimpleName());
        }
    }
}
