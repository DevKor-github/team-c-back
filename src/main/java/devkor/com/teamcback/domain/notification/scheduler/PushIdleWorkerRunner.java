package devkor.com.teamcback.domain.notification.scheduler;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushIdleWorkerRunner {
    private final PushWorkSignals signals;
    private final PushWorkScheduleReader schedules;
    private final Clock clock;
    @Value("${push.storage:mysql}") private String storage;
    @Value("${push.worker.enabled:false}") private boolean dispatchEnabled;
    @Value("${push.receipt-worker.enabled:false}") private boolean receiptEnabled;
    @Value("${push.survey.worker-enabled:false}") private boolean surveyEnabled;

    public void run(PushWorkSignals.Work work, Runnable task) {
        if (!"neon".equals(storage)) { task.run(); return; }
        String token = signals.claim(work);
        if (token == null) return; // Do not enter a Postgres transaction while idle.
        try {
            task.run();
            LocalDateTime next = schedules.next(work);
            if (next != null) {
                // Avoid busy spinning when a row is locked or another batch remains.
                LocalDateTime earliest = LocalDateTime.now(clock).plusSeconds(5);
                signals.wake(work, next.isBefore(earliest) ? earliest : next);
            }
        } catch (RuntimeException error) {
            signals.wake(work, LocalDateTime.now(clock).plusMinutes(15));
            log.warn("Push worker deferred. work={}, errorType={}", work, error.getClass().getSimpleName());
        } finally {
            signals.release(work, token);
        }
    }

    // A lost hint/process crash is recovered without holding Neon awake between probes.
    // Every instance uses the same wall-clock hour and Redis keys/leases.
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${push.idle.reconcile-cron:0 0 * * * *}")
    public void reconcile() {
        LocalDateTime now = LocalDateTime.now(clock);
        if (dispatchEnabled) signals.wake(PushWorkSignals.Work.DISPATCH, now);
        if (receiptEnabled) signals.wake(PushWorkSignals.Work.RECEIPT, now);
        if (surveyEnabled) signals.wake(PushWorkSignals.Work.SURVEY, now);
    }
}
