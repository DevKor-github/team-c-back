package devkor.com.teamcback.domain.notification.scheduler;

import devkor.com.teamcback.domain.notification.service.PushReceiptWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "push.receipt-worker", name = "enabled", havingValue = "true")
public class PushReceiptWorkerScheduler {

    private final PushReceiptWorker pushReceiptWorker;

    @Scheduled(fixedDelayString = "${push.receipt-worker.fixed-delay-ms:60000}")
    public void checkPendingReceipts() {
        pushReceiptWorker.checkPendingReceipts();
    }
}
