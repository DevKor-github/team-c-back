package devkor.com.teamcback.domain.notification.scheduler;

import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import static devkor.com.teamcback.domain.notification.scheduler.PushWorkSignals.Work.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PushWorkSignalsFallbackTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test void redisOutageDoesNotRestoreContinuousPostgresPolling() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        when(factory.getConnection()).thenThrow(new IllegalStateException("local outage"));
        PushWorkSignals signals = new PushWorkSignals(factory, clock, "neon", "test", "LIVE");
        assertThat(signals.claim(DISPATCH)).isEqualTo("local");
        for (int tick = 0; tick < 1000; tick++) assertThat(signals.claim(DISPATCH)).isNull();
        signals.wake(DISPATCH, LocalDateTime.now(clock));
        assertThat(signals.claim(DISPATCH)).isEqualTo("local");
        assertThat(signals.claim(DISPATCH)).isNull();
    }
}
