package devkor.com.teamcback.domain.usagesurvey.dto.response;

import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyQuestion;
import java.util.List;

public record UsageSurveyStatusRes(
        List<UsageSurveyQuestion> answeredQuestionKeys,
        int rewardPoint
) {
}
