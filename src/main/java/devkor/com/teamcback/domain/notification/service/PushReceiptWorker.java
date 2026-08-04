package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.client.ExpoPushClient;
import devkor.com.teamcback.domain.notification.client.ExpoPushClientException;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoReceiptResponse;
import devkor.com.teamcback.domain.notification.dto.worker.PushReceiptItem;
import devkor.com.teamcback.domain.notification.dto.worker.PushReceiptWorkerResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushReceiptWorker {

    private final PushReceiptClaimService pushReceiptClaimService;
    private final ExpoPushClient expoPushClient;

    public PushReceiptWorkerResult checkPendingReceipts() {
        List<PushReceiptItem> items = pushReceiptClaimService.claimDueReceipts();
        if (items.isEmpty()) {
            return PushReceiptWorkerResult.empty();
        }

        try {
            ExpoReceiptResponse response = expoPushClient.getReceipts(items.stream()
                    .map(PushReceiptItem::expoTicketId)
                    .toList());
            pushReceiptClaimService.recordReceipts(items, response.data());
            return new PushReceiptWorkerResult(items.size(), items.size());
        } catch (ExpoPushClientException e) {
            pushReceiptClaimService.recordClientError(
                    items,
                    e.isRetryable(),
                    e.getHttpStatus() == null ? e.getMessage() : "http_" + e.getHttpStatus()
            );
            log.warn(
                    "Expo push receipt request failed. retryable={}, itemCount={}",
                    e.isRetryable(),
                    items.size()
            );
            return new PushReceiptWorkerResult(items.size(), 0);
        }
    }
}
