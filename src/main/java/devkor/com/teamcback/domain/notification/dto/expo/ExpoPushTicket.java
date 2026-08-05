package devkor.com.teamcback.domain.notification.dto.expo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpoPushTicket(
        String status,
        String id,
        String message,
        ExpoPushErrorDetails details
) {
}
