package devkor.com.teamcback.domain.notification.client;

import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushRequest;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushResponse;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoReceiptRequest;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoReceiptResponse;
import feign.FeignException;
import feign.RetryableException;
import feign.codec.DecodeException;
import feign.codec.EncodeException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpoPushClient {

    private static final int MAX_SEND_REQUEST_COUNT = 100;
    private static final int MAX_RECEIPT_ID_COUNT = 1000;

    private final ExpoPushApi expoPushApi;

    public ExpoPushResponse send(List<ExpoPushRequest> requests) {
        validateSendRequests(requests);

        try {
            ExpoPushResponse response = expoPushApi.send(requests);
            validateSendResponse(response, requests.size());
            return response;
        } catch (ExpoPushClientException e) {
            throw e;
        } catch (RetryableException e) {
            throw requestFailed(null, true, e);
        } catch (DecodeException e) {
            throw parsingFailed(e);
        } catch (EncodeException e) {
            throw invalidInput(e);
        } catch (FeignException e) {
            throw requestFailed(e.status(), isRetryable(e.status()), e);
        }
    }

    public ExpoReceiptResponse getReceipts(List<String> ticketIds) {
        validateReceiptIds(ticketIds);

        try {
            ExpoReceiptResponse response = expoPushApi.getReceipts(new ExpoReceiptRequest(ticketIds));
            if (response == null) {
                throw parsingFailed();
            }
            return response;
        } catch (ExpoPushClientException e) {
            throw e;
        } catch (RetryableException e) {
            throw requestFailed(null, true, e);
        } catch (DecodeException e) {
            throw parsingFailed(e);
        } catch (EncodeException e) {
            throw invalidInput(e);
        } catch (FeignException e) {
            throw requestFailed(e.status(), isRetryable(e.status()), e);
        }
    }

    private void validateSendResponse(
            ExpoPushResponse response,
            int requestCount
    ) {
        if (response == null || response.data() == null || response.data().size() != requestCount) {
            throw parsingFailed();
        }
    }

    private void validateSendRequests(List<ExpoPushRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw invalidInput();
        }

        if (requests.size() > MAX_SEND_REQUEST_COUNT) {
            throw invalidInput();
        }

        boolean hasInvalidRequest = requests.stream()
                .anyMatch(request -> request == null || !hasText(request.to()));

        if (hasInvalidRequest) {
            throw invalidInput();
        }
    }

    private void validateReceiptIds(List<String> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw invalidInput();
        }

        if (ticketIds.size() > MAX_RECEIPT_ID_COUNT) {
            throw invalidInput();
        }

        boolean hasBlankTicketId = ticketIds.stream()
                .anyMatch(ticketId -> !hasText(ticketId));

        if (hasBlankTicketId) {
            throw invalidInput();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isRetryable(int status) {
        return status == HttpStatus.TOO_MANY_REQUESTS.value()
                || (status >= 500 && status < 600);
    }

    private ExpoPushClientException invalidInput() {
        return new ExpoPushClientException(
                "Invalid Expo push client request",
                HttpStatus.BAD_REQUEST.value(),
                false
        );
    }

    private ExpoPushClientException invalidInput(Throwable cause) {
        return new ExpoPushClientException(
                "Invalid Expo push client request",
                HttpStatus.BAD_REQUEST.value(),
                false,
                cause
        );
    }

    private ExpoPushClientException parsingFailed() {
        return new ExpoPushClientException(
                "Failed to parse Expo push response",
                null,
                false
        );
    }

    private ExpoPushClientException parsingFailed(Throwable cause) {
        return new ExpoPushClientException(
                "Failed to parse Expo push response",
                null,
                false,
                cause
        );
    }

    private ExpoPushClientException requestFailed(
            Integer httpStatus,
            boolean retryable,
            Throwable cause
    ) {
        return new ExpoPushClientException(
                "Expo push request failed",
                httpStatus,
                retryable,
                cause
        );
    }
}
