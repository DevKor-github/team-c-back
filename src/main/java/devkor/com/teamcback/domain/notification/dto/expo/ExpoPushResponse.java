package devkor.com.teamcback.domain.notification.dto.expo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpoPushResponse(
        List<ExpoPushTicket> data
) {
}
