package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushDispatchStatus;
import lombok.Getter;

@Getter
public class PushDispatchEnqueueRes {

    private final Long dispatchId;
    private final int recipientCount;
    private final PushDispatchStatus status;
    private final String idempotencyKey;

    public PushDispatchEnqueueRes(PushDispatch dispatch) {
        this.dispatchId = dispatch.getPushDispatchId();
        this.recipientCount = dispatch.getRecipientCount();
        this.status = dispatch.getStatus();
        this.idempotencyKey = dispatch.getIdempotencyKey();
    }
}
