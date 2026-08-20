package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.entity.type.SurveyReminderSuppressedBy;
import java.time.LocalDateTime;

public record SurveyReminderRes(
        String surveyKey,
        boolean scheduled,
        LocalDateTime scheduledAt,
        SurveyReminderSuppressedBy suppressedBy
) {
}
