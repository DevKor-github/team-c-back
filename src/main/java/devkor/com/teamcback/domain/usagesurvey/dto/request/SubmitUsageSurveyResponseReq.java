package devkor.com.teamcback.domain.usagesurvey.dto.request;

import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyQuestion;

public record SubmitUsageSurveyResponseReq(
        UsageSurveyQuestion questionKey,
        String optionKey
) {
}
