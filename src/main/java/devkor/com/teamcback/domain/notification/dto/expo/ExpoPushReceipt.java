package devkor.com.teamcback.domain.notification.dto.expo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpoPushReceipt(
        String status,
        String message,
        ExpoPushErrorDetails details
) {
}
