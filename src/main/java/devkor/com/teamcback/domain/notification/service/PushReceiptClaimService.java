package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.config.PushReceiptWorkerProperties;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushReceipt;
import devkor.com.teamcback.domain.notification.dto.worker.PushReceiptItem;
import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushMessage;
import devkor.com.teamcback.domain.notification.entity.type.PushMessageStatus;
import devkor.com.teamcback.domain.notification.repository.PushDispatchRepository;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushReceiptClaimService {

    private static final String RECEIPT_STATUS_OK = "ok";
    private static final String RECEIPT_STATUS_ERROR = "error";
    private static final String RECEIPT_STATUS_NOT_READY = "not_ready";
    private static final String CLIENT_ERROR_STATUS = "client_error";
    private static final String DEVICE_NOT_REGISTERED_ERROR = "DeviceNotRegistered";

    private final PushMessageRepository pushMessageRepository;
    private final PushInstallationRepository pushInstallationRepository;
    private final PushDispatchRepository pushDispatchRepository;
    private final PushReceiptWorkerProperties pushReceiptWorkerProperties;
    private final Clock clock;

    @Transactional
    public List<PushReceiptItem> claimDueReceipts() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<PushMessage> messages = pushMessageRepository.findDueReceiptPendingForUpdateSkipLocked(
                now,
                pushReceiptWorkerProperties.batchSize()
        );

        if (messages.isEmpty()) {
            return List.of();
        }

        Set<Long> dispatchIds = new HashSet<>();
        List<PushReceiptItem> receiptItems = messages.stream()
                .map(message -> claimMessage(message, now, dispatchIds))
                .filter(item -> item != null)
                .toList();

        refreshDispatchStatuses(dispatchIds, now);
        return receiptItems;
    }

    @Transactional
    public void recordReceipts(
            List<PushReceiptItem> items,
            Map<String, ExpoPushReceipt> receiptMap
    ) {
        if (items.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, PushMessage> messageMap = findMessageMap(items);
        Set<Long> dispatchIds = new HashSet<>();

        for (PushReceiptItem item : items) {
            PushMessage message = messageMap.get(item.pushMessageId());
            if (message == null) {
                continue;
            }

            ExpoPushReceipt receipt = receiptMap == null ? null : receiptMap.get(item.expoTicketId());
            if (receipt == null) {
                scheduleRetry(message, RECEIPT_STATUS_NOT_READY, "receipt_not_ready", now);
            } else if (isDeviceNotRegisteredReceipt(receipt)) {
                message.recordReceipt(
                        receipt.status(),
                        receiptError(receipt),
                        now
                );
                pushInstallationRepository.findById(message.getPushInstallationId())
                        .ifPresent(installation -> installation.deactivate(now));
            } else {
                message.recordReceipt(
                        receipt.status(),
                        receiptError(receipt),
                        now
                );
            }
            dispatchIds.add(item.pushDispatchId());
        }

        refreshDispatchStatuses(dispatchIds, now);
    }

    @Transactional
    public void recordClientError(
            List<PushReceiptItem> items,
            boolean retryable,
            String receiptError
    ) {
        if (items.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, PushMessage> messageMap = findMessageMap(items);
        Set<Long> dispatchIds = new HashSet<>();

        items.forEach(item -> {
            PushMessage message = messageMap.get(item.pushMessageId());
            if (message == null) {
                return;
            }
            if (retryable) {
                scheduleRetry(message, CLIENT_ERROR_STATUS, truncate(receiptError), now);
            } else {
                message.recordReceipt(
                        CLIENT_ERROR_STATUS,
                        truncate(receiptError),
                        now
                );
            }
            dispatchIds.add(item.pushDispatchId());
        });

        refreshDispatchStatuses(dispatchIds, now);
    }

    private PushReceiptItem claimMessage(
            PushMessage message,
            LocalDateTime now,
            Set<Long> dispatchIds
    ) {
        dispatchIds.add(message.getDispatch().getPushDispatchId());

        if (isReceiptExpired(message, now)) {
            message.recordReceiptExpired(now);
            return null;
        }

        message.markReceiptChecking(now);
        return new PushReceiptItem(
                message.getPushMessageId(),
                message.getDispatch().getPushDispatchId(),
                message.getExpoTicketId()
        );
    }

    private void scheduleRetry(
            PushMessage message,
            String receiptStatus,
            String receiptError,
            LocalDateTime now
    ) {
        if (isReceiptExpired(message, now)) {
            message.recordReceiptExpired(now);
            return;
        }

        message.scheduleReceiptRetry(
                receiptStatus,
                receiptError,
                pushReceiptWorkerProperties.maxReceiptAttempts(),
                now.plusMinutes(nextBackoffMinutes(message.getReceiptAttempts())),
                now
        );
    }

    private long nextBackoffMinutes(int currentReceiptAttempts) {
        int retryNumber = Math.max(0, currentReceiptAttempts);
        return 1L << Math.min(retryNumber, 2);
    }

    private boolean isReceiptExpired(
            PushMessage message,
            LocalDateTime now
    ) {
        return message.getSentAt() == null
                || message.getSentAt().plusHours(24).isBefore(now);
    }

    private Map<Long, PushMessage> findMessageMap(List<PushReceiptItem> items) {
        List<Long> messageIds = items.stream()
                .map(PushReceiptItem::pushMessageId)
                .toList();

        return pushMessageRepository.findAllByPushMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.toMap(
                        PushMessage::getPushMessageId,
                        message -> message
                ));
    }

    private void refreshDispatchStatuses(
            Collection<Long> dispatchIds,
            LocalDateTime now
    ) {
        if (dispatchIds.isEmpty()) {
            return;
        }

        Map<Long, EnumMap<PushMessageStatus, Long>> countsByDispatchId = new HashMap<>();
        pushMessageRepository.countStatusesByDispatchIds(dispatchIds)
                .forEach(count -> countsByDispatchId
                        .computeIfAbsent(
                                count.getDispatchId(),
                                ignored -> new EnumMap<>(PushMessageStatus.class)
                        )
                        .put(count.getStatus(), count.getCount()));

        pushDispatchRepository.findAllById(dispatchIds)
                .forEach(dispatch -> updateDispatchStatus(dispatch, countsByDispatchId, now));
    }

    private void updateDispatchStatus(
            PushDispatch dispatch,
            Map<Long, EnumMap<PushMessageStatus, Long>> countsByDispatchId,
            LocalDateTime now
    ) {
        EnumMap<PushMessageStatus, Long> counts = countsByDispatchId.getOrDefault(
                dispatch.getPushDispatchId(),
                new EnumMap<>(PushMessageStatus.class)
        );

        dispatch.updateStatusFromMessageSummary(
                count(counts, PushMessageStatus.QUEUED)
                        + count(counts, PushMessageStatus.TICKET_RECEIVED)
                        + count(counts, PushMessageStatus.RECEIPT_PENDING),
                count(counts, PushMessageStatus.SENDING),
                count(counts, PushMessageStatus.DELIVERED),
                count(counts, PushMessageStatus.FAILED),
                now
        );
    }

    private long count(
            EnumMap<PushMessageStatus, Long> counts,
            PushMessageStatus status
    ) {
        return counts.getOrDefault(status, 0L);
    }

    private boolean isDeviceNotRegisteredReceipt(ExpoPushReceipt receipt) {
        return receipt != null
                && RECEIPT_STATUS_ERROR.equals(receipt.status())
                && receipt.details() != null
                && DEVICE_NOT_REGISTERED_ERROR.equals(receipt.details().error());
    }

    private String receiptError(ExpoPushReceipt receipt) {
        if (receipt == null || RECEIPT_STATUS_OK.equals(receipt.status())) {
            return null;
        }

        if (receipt.details() != null && receipt.details().error() != null) {
            return truncate(receipt.details().error());
        }

        return truncate(receipt.message());
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1024) {
            return value;
        }
        return value.substring(0, 1024);
    }
}
