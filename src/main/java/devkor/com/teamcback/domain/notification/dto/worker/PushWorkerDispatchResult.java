package devkor.com.teamcback.domain.notification.dto.worker;

public record PushWorkerDispatchResult(
        int claimedCount,
        int sentCount
) {

    public static PushWorkerDispatchResult empty() {
        return new PushWorkerDispatchResult(0, 0);
    }
}
