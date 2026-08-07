package devkor.com.teamcback.domain.notification.client;

import lombok.Getter;

@Getter
public class ExpoPushClientException extends RuntimeException {

    private final Integer httpStatus;
    private final boolean retryable;

    public ExpoPushClientException(
            String message,
            Integer httpStatus,
            boolean retryable
    ) {
        super(message);
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }

    public ExpoPushClientException(
            String message,
            Integer httpStatus,
            boolean retryable,
            Throwable cause
    ) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }
}
