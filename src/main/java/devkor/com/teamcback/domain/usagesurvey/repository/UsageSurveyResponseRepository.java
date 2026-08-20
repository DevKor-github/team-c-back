package devkor.com.teamcback.domain.usagesurvey.repository;

import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyQuestion;
import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyResponse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageSurveyResponseRepository extends JpaRepository<UsageSurveyResponse, Long> {
    boolean existsByUserUserIdAndQuestionKey(Long userId, UsageSurveyQuestion questionKey);

    List<UsageSurveyResponse> findAllByUserUserId(Long userId);
}
