package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.client.ExpoPushClient;
import devkor.com.teamcback.domain.notification.client.ExpoPushClientException;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushResponse;
import devkor.com.teamcback.domain.notification.dto.worker.PushSendItem;
import devkor.com.teamcback.domain.notification.dto.worker.PushWorkerDispatchResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushMessageDispatchWorker {

    private final PushMessageClaimService pushMessageClaimService;
    private final ExpoPushClient expoPushClient;

    public PushWorkerDispatchResult dispatchPending() {
        List<PushSendItem> items = pushMessageClaimService.claimDueMessages();
        if (items.isEmpty()) {
            return PushWorkerDispatchResult.empty();
        }

        try {
            ExpoPushResponse response = expoPushClient.send(items.stream()
                    .map(PushSendItem::request)
                    .toList());
            pushMessageClaimService.recordTickets(items, response.data());
            return new PushWorkerDispatchResult(items.size(), items.size());
        } catch (ExpoPushClientException e) {
            pushMessageClaimService.recordClientError(
                    items,
                    e.isRetryable(),
                    e.getHttpStatus() == null ? e.getMessage() : "http_" + e.getHttpStatus()
            );
            log.warn(
                    "Expo push send failed. retryable={}, itemCount={}",
                    e.isRetryable(),
                    items.size()
            );
            return new PushWorkerDispatchResult(items.size(), 0);
        }
    }
}
