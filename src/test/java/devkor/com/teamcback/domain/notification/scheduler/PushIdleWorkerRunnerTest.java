package devkor.com.teamcback.domain.notification.scheduler;

import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static devkor.com.teamcback.domain.notification.scheduler.PushWorkSignals.Work.*;
import static org.mockito.Mockito.*;

class PushIdleWorkerRunnerTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final PushWorkSignals signals = mock(PushWorkSignals.class);
    private final PushWorkScheduleReader schedules = mock(PushWorkScheduleReader.class);
    private final Runnable task = mock(Runnable.class);
    private PushIdleWorkerRunner runner;

    @BeforeEach void setup() {
        runner = new PushIdleWorkerRunner(signals, schedules, clock);
        ReflectionTestUtils.setField(runner, "storage", "neon");
    }

    @Test void thousandsOfIdleTicksNeverEnterPostgresOrInvokeWorker() {
        for (int i = 0; i < 1000; i++) runner.run(DISPATCH, task);
        verifyNoInteractions(task, schedules);
        verify(signals, never()).release(any(), any());
    }

    @Test void dueWorkRunsAndSchedulesOnlyTheNextActualRetry() {
        when(signals.claim(DISPATCH)).thenReturn("lease");
        LocalDateTime next = LocalDateTime.now(clock).plusMinutes(15);
        when(schedules.next(DISPATCH)).thenReturn(next);
        runner.run(DISPATCH, task);
        verify(task).run();
        verify(signals).wake(DISPATCH, next);
        verify(signals).release(DISPATCH, "lease");
    }

    @Test void emptyQueueDoesNotReschedulePolling() {
        when(signals.claim(RECEIPT)).thenReturn("lease");
        runner.run(RECEIPT, task);
        verify(task).run();
        verify(signals, never()).wake(any(), any());
        verify(signals).release(RECEIPT, "lease");
    }

    @Test void failureDoesNotRestoreFiveSecondPollingAndLeaseIsReleased() {
        when(signals.claim(SURVEY)).thenReturn("lease");
        doThrow(new IllegalStateException()).when(task).run();
        runner.run(SURVEY, task);
        verify(signals).wake(SURVEY, LocalDateTime.now(clock).plusMinutes(15));
        verify(signals).release(SURVEY, "lease");
        verifyNoInteractions(schedules);
    }

    @Test void legacyMysqlWorkerKeepsItsExistingBehavior() {
        ReflectionTestUtils.setField(runner, "storage", "mysql");
        runner.run(DISPATCH, task);
        verify(task).run();
        verifyNoInteractions(signals, schedules);
    }
}
