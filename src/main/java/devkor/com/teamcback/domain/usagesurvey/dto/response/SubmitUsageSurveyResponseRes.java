package devkor.com.teamcback.domain.usagesurvey.dto.response;

import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyQuestion;
import devkor.com.teamcback.global.response.ScoreUpdateResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SubmitUsageSurveyResponseRes implements ScoreUpdateResponse {
    private final UsageSurveyQuestion questionKey;
    private final String optionKey;
    private final int rewardPoint;

    @Setter
    private boolean levelUp;
    @Setter
    private Long currentScore;
    @Setter
    private boolean scoreGained;

    public SubmitUsageSurveyResponseRes(
            UsageSurveyQuestion questionKey,
            String optionKey,
            int rewardPoint
    ) {
        this.questionKey = questionKey;
        this.optionKey = optionKey;
        this.rewardPoint = rewardPoint;
    }
}
