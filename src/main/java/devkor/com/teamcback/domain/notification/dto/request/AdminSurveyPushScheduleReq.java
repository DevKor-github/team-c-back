package devkor.com.teamcback.domain.notification.dto.request;

import java.time.LocalDateTime;

public record AdminSurveyPushScheduleReq(
        LocalDateTime startNotificationAt,
        LocalDateTime deadlineNotificationAt,
        Integer rewardPoint
) {
}
