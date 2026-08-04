package devkor.com.teamcback.domain.notification.scheduler;

import devkor.com.teamcback.domain.notification.service.PushMessageRecoveryWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "push.recovery-worker", name = "enabled", havingValue = "true")
public class PushMessageRecoveryScheduler {

    private final PushMessageRecoveryWorker pushMessageRecoveryWorker;

    @Scheduled(cron = "${push.recovery-worker.cron:0 0 4 * * *}")
    public void recoverStaleSendingMessages() {
        pushMessageRecoveryWorker.recoverStaleSendingMessages();
    }
}
