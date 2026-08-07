package devkor.com.teamcback.domain.notification.dto.worker;

public record PushReceiptWorkerResult(
        int claimedCount,
        int checkedCount
) {

    public static PushReceiptWorkerResult empty() {
        return new PushReceiptWorkerResult(0, 0);
    }
}
