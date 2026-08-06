package devkor.com.teamcback.domain.notification.dto.response;

import devkor.com.teamcback.domain.notification.entity.SurveyPushSchedule;
import devkor.com.teamcback.domain.notification.entity.type.SurveyPushScheduleStatus;
import java.time.LocalDateTime;

public record AdminSurveyPushScheduleStageRes(
        SurveyPushScheduleStatus status,
        LocalDateTime scheduledAt
) {

    public static AdminSurveyPushScheduleStageRes from(SurveyPushSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return new AdminSurveyPushScheduleStageRes(
                schedule.getStatus(),
                schedule.getScheduledAt()
        );
    }
}
