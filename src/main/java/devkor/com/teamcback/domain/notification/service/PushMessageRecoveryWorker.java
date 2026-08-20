package devkor.com.teamcback.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushMessageRecoveryWorker {

    private final PushMessageRecoveryService pushMessageRecoveryService;

    public int recoverStaleSendingMessages() {
        int recoveredCount = pushMessageRecoveryService.recoverStaleSendingMessages();
        if (recoveredCount > 0) {
            log.warn("Recovered stale SENDING push messages. count={}", recoveredCount);
        }
        return recoveredCount;
    }
}
