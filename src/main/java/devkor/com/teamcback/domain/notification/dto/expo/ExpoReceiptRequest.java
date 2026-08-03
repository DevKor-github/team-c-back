package devkor.com.teamcback.domain.notification.dto.expo;

import java.util.List;

public record ExpoReceiptRequest(
        List<String> ids
) {
}
