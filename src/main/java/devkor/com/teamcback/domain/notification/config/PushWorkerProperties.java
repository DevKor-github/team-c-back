package devkor.com.teamcback.domain.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "push.worker")
public record PushWorkerProperties(
        boolean enabled,
        int batchSize,
        int maxSendAttempts,
        long retryDelayMs
) {

    private static final int MAX_EXPO_BATCH_SIZE = 100;

    public PushWorkerProperties {
        if (batchSize <= 0 || batchSize > MAX_EXPO_BATCH_SIZE) {
            batchSize = MAX_EXPO_BATCH_SIZE;
        }
        if (maxSendAttempts <= 0) {
            maxSendAttempts = 3;
        }
        if (retryDelayMs <= 0) {
            retryDelayMs = 30_000L;
        }
    }
}
