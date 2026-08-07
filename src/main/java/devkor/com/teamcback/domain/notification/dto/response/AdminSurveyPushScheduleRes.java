package devkor.com.teamcback.domain.notification.dto.response;

public record AdminSurveyPushScheduleRes(
        String surveyKey,
        int rewardPoint,
        AdminSurveyPushScheduleStageRes started,
        AdminSurveyPushScheduleStageRes dMinus3,
        AdminSurveyPushScheduleStageRes deadline
) {
}
