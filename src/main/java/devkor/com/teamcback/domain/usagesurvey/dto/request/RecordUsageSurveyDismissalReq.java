package devkor.com.teamcback.domain.usagesurvey.dto.request;

import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyDismissReason;
import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyQuestion;

public record RecordUsageSurveyDismissalReq(
        UsageSurveyQuestion questionKey,
        UsageSurveyDismissReason reason
) {
}
