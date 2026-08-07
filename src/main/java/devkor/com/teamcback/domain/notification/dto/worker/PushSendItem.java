package devkor.com.teamcback.domain.notification.dto.worker;

import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushRequest;

public record PushSendItem(
        Long pushMessageId,
        Long pushDispatchId,
        ExpoPushRequest request
) {
}
