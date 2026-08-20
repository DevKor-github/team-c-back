package devkor.com.teamcback.domain.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "push.recovery-worker")
public record PushRecoveryWorkerProperties(
        boolean enabled,
        String cron,
        long staleThresholdMinutes,
        int batchSize
) {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final long DEFAULT_STALE_THRESHOLD_MINUTES = 30L;

    public PushRecoveryWorkerProperties {
        if (staleThresholdMinutes <= 0) {
            staleThresholdMinutes = DEFAULT_STALE_THRESHOLD_MINUTES;
        }
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
    }
}
