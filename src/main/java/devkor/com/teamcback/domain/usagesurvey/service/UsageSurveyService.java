package devkor.com.teamcback.domain.usagesurvey.service;

import static devkor.com.teamcback.global.response.ResultCode.ALREADY_ANSWERED_USAGE_SURVEY;
import static devkor.com.teamcback.global.response.ResultCode.INVALID_USAGE_SURVEY_OPTION;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;

import devkor.com.teamcback.domain.usagesurvey.dto.request.RecordUsageSurveyDismissalReq;
import devkor.com.teamcback.domain.usagesurvey.dto.request.SubmitUsageSurveyResponseReq;
import devkor.com.teamcback.domain.usagesurvey.dto.response.SubmitUsageSurveyResponseRes;
import devkor.com.teamcback.domain.usagesurvey.dto.response.UsageSurveyStatusRes;
import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyDismissReason;
import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyDismissal;
import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyQuestion;
import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyResponse;
import devkor.com.teamcback.domain.usagesurvey.repository.UsageSurveyDismissalRepository;
import devkor.com.teamcback.domain.usagesurvey.repository.UsageSurveyResponseRepository;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.annotation.UpdateScore;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsageSurveyService {
    public static final int REWARD_POINT = 10;

    private static final Map<UsageSurveyQuestion, Set<String>> ALLOWED_OPTIONS = Map.of(
            UsageSurveyQuestion.INSTALL_REASON, Set.of(
                    "COMPLEX_CAMPUS_ROUTES",
                    "SCHOOL_INFO",
                    "FACILITIES",
                    "LOUNGE_CROWDING",
                    "BUILDING_INTERIOR"
            ),
            UsageSurveyQuestion.RECENT_USE_REASON, Set.of(
                    "ROUTE",
                    "CROWDING",
                    "FACILITIES",
                    "MEAL_SHUTTLE",
                    "OTHER"
            ),
            UsageSurveyQuestion.DESIRED_FEATURE, Set.of(
                    "SCHEDULE_ROUTE",
                    "STUDY_ROOM_LINK",
                    "CUP_AVAILABILITY",
                    "QUIET_ALERT",
                    "COMMUNITY"
            ),
            UsageSurveyQuestion.DISAPPOINTMENT, Set.of(
                    "STALE_INFO",
                    "NAVIGATION_DIFFICULTY",
                    "MISSING_REVIEW_OUTLET",
                    "CROWD_MISMATCH",
                    "OTHER"
            )
    );

    private final UsageSurveyResponseRepository responseRepository;
    private final UsageSurveyDismissalRepository dismissalRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UsageSurveyStatusRes getStatus(Long userId) {
        findUser(userId);
        List<UsageSurveyQuestion> answeredQuestionKeys = responseRepository
                .findAllByUserUserId(userId)
                .stream()
                .map(UsageSurveyResponse::getQuestionKey)
                .distinct()
                .toList();
        return new UsageSurveyStatusRes(answeredQuestionKeys, REWARD_POINT);
    }

    @Transactional
    @UpdateScore(addScore = REWARD_POINT)
    public SubmitUsageSurveyResponseRes submitResponse(
            Long userId,
            SubmitUsageSurveyResponseReq req
    ) {
        validateResponse(req);
        if (responseRepository.existsByUserUserIdAndQuestionKey(userId, req.questionKey())) {
            throw new GlobalException(ALREADY_ANSWERED_USAGE_SURVEY);
        }

        User user = findUser(userId);
        responseRepository.save(new UsageSurveyResponse(
                user,
                req.questionKey(),
                req.optionKey()
        ));
        return new SubmitUsageSurveyResponseRes(
                req.questionKey(),
                req.optionKey(),
                REWARD_POINT
        );
    }

    @Transactional
    public void recordDismissal(
            Long userId,
            RecordUsageSurveyDismissalReq req
    ) {
        if (req == null || req.questionKey() == null) {
            throw new GlobalException(INVALID_USAGE_SURVEY_OPTION);
        }
        User user = findUser(userId);
        UsageSurveyDismissReason reason = req.reason() == null
                ? UsageSurveyDismissReason.LATER
                : req.reason();
        dismissalRepository.save(new UsageSurveyDismissal(
                user,
                req.questionKey(),
                reason
        ));
    }

    private void validateResponse(SubmitUsageSurveyResponseReq req) {
        if (req == null || req.questionKey() == null || req.optionKey() == null) {
            throw new GlobalException(INVALID_USAGE_SURVEY_OPTION);
        }
        Set<String> options = ALLOWED_OPTIONS.get(req.questionKey());
        if (options == null || !options.contains(req.optionKey())) {
            throw new GlobalException(INVALID_USAGE_SURVEY_OPTION);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }
}
