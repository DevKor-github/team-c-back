package devkor.com.teamcback.domain.notification.scheduler;

import devkor.com.teamcback.domain.notification.service.PushMessageDispatchWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "push.worker", name = "enabled", havingValue = "true")
public class PushMessageWorkerScheduler {

    private final PushMessageDispatchWorker pushMessageDispatchWorker;

    @Scheduled(fixedDelayString = "${push.worker.fixed-delay-ms:5000}")
    public void dispatchQueuedMessages() {
        pushMessageDispatchWorker.dispatchPending();
    }
}
