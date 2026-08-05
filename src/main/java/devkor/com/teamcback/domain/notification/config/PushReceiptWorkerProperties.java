package devkor.com.teamcback.domain.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "push.receipt-worker")
public record PushReceiptWorkerProperties(
        boolean enabled,
        int batchSize,
        int maxReceiptAttempts
) {

    private static final int MAX_EXPO_RECEIPT_BATCH_SIZE = 1000;

    public PushReceiptWorkerProperties {
        if (batchSize <= 0 || batchSize > MAX_EXPO_RECEIPT_BATCH_SIZE) {
            batchSize = MAX_EXPO_RECEIPT_BATCH_SIZE;
        }
        if (maxReceiptAttempts <= 0) {
            maxReceiptAttempts = 3;
        }
    }
}
