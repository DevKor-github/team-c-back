package devkor.com.teamcback.domain.notification.scheduler;

import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static devkor.com.teamcback.domain.notification.scheduler.PushWorkSignals.Work.*;
import static org.assertj.core.api.Assertions.*;

/** Uses only the dedicated local disposable Redis on port 55440. */
@EnabledIfEnvironmentVariable(named = "KODAERO_PUSH_REDIS_TEST", matches = "true")
class PushWorkSignalsIntegrationTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private LettuceConnectionFactory factory;
    private PushWorkSignals first;
    private PushWorkSignals second;
    private StringRedisTemplate redis;
    private String namespace;

    @BeforeEach void setup() {
        factory = new LettuceConnectionFactory("127.0.0.1", 55440);
        factory.afterPropertiesSet();
        factory.start();
        namespace = "test-" + UUID.randomUUID();
        first = new PushWorkSignals(factory, clock, "neon", namespace, "LIVE");
        second = new PushWorkSignals(factory, clock, "neon", namespace, "LIVE");
        redis = new StringRedisTemplate(factory);
    }

    @AfterEach void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
        redis.delete(redis.keys("push:wake:v1:" + namespace + ":*"));
        factory.destroy();
    }

    @Test void noSignalAndFutureReceiptNeverWakeDatabase() {
        assertThat(first.claim(DISPATCH)).isNull();
        first.wake(RECEIPT, LocalDateTime.now(clock).plusMinutes(15));
        assertThat(first.claim(RECEIPT)).isNull();
    }

    @Test void wakeDuringExecutionSurvivesAndOnlyOneInstanceHoldsLease() {
        first.wake(DISPATCH, LocalDateTime.now(clock));
        String lease = first.claim(DISPATCH);
        assertThat(lease).isNotNull();
        second.wake(DISPATCH, LocalDateTime.now(clock));
        assertThat(second.claim(DISPATCH)).isNull();
        second.release(DISPATCH, "wrong-token");
        assertThat(second.claim(DISPATCH)).isNull();
        first.release(DISPATCH, lease);
        String nextLease = second.claim(DISPATCH);
        assertThat(nextLease).isNotNull();
        second.release(DISPATCH, nextLease);
        assertThat(first.claim(DISPATCH)).isNull();
    }

    @Test void laterReconciliationCannotOverwriteEarlierWork() {
        first.wake(DISPATCH, LocalDateTime.now(clock));
        second.wake(DISPATCH, LocalDateTime.now(clock).plusHours(1));
        assertThat(first.claim(DISPATCH)).isNotNull();
    }

    @Test void rolledBackEnqueueDoesNotWakeWorkerButCommittedEnqueueDoes() {
        TransactionSynchronizationManager.initSynchronization();
        first.wakeAfterCommit(DISPATCH, LocalDateTime.now(clock));
        assertThat(second.claim(DISPATCH)).isNull();
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        TransactionSynchronizationManager.clearSynchronization();
        assertThat(second.claim(DISPATCH)).isNull();
        TransactionSynchronizationManager.initSynchronization();
        first.wakeAfterCommit(DISPATCH, LocalDateTime.now(clock));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        TransactionSynchronizationManager.clearSynchronization();
        assertThat(second.claim(DISPATCH)).isNotNull();
    }

    @Test void surveySignalsAreAudienceScopedAndDispatchSignalsShared() {
        PushWorkSignals internal = new PushWorkSignals(factory, clock, "neon", namespace, "INTERNAL_TEST");
        first.wake(SURVEY, LocalDateTime.now(clock));
        assertThat(internal.claim(SURVEY)).isNull();
        first.wake(DISPATCH, LocalDateTime.now(clock));
        assertThat(internal.claim(DISPATCH)).isNotNull();
    }

    @Test void processCrashLeaseExpiresWithoutLosingNewWork() {
        first.wake(DISPATCH, LocalDateTime.now(clock));
        assertThat(first.claim(DISPATCH)).isNotNull();
        second.wake(DISPATCH, LocalDateTime.now(clock));
        redis.delete(first.key(DISPATCH) + ":lock"); // equivalent to Redis lease expiry
        assertThat(second.claim(DISPATCH)).isNotNull();
    }
}
