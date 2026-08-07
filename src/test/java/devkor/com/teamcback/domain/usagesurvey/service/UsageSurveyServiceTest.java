package devkor.com.teamcback.domain.usagesurvey.service;

import static devkor.com.teamcback.global.response.ResultCode.ALREADY_ANSWERED_USAGE_SURVEY;
import static devkor.com.teamcback.global.response.ResultCode.INVALID_USAGE_SURVEY_OPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.usagesurvey.dto.request.RecordUsageSurveyDismissalReq;
import devkor.com.teamcback.domain.usagesurvey.dto.request.SubmitUsageSurveyResponseReq;
import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyDismissReason;
import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyDismissal;
import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyQuestion;
import devkor.com.teamcback.domain.usagesurvey.entity.UsageSurveyResponse;
import devkor.com.teamcback.domain.usagesurvey.repository.UsageSurveyDismissalRepository;
import devkor.com.teamcback.domain.usagesurvey.repository.UsageSurveyResponseRepository;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.annotation.UpdateScore;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsageSurveyServiceTest {
    @Mock
    private UsageSurveyResponseRepository responseRepository;
    @Mock
    private UsageSurveyDismissalRepository dismissalRepository;
    @Mock
    private UserRepository userRepository;

    private UsageSurveyService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new UsageSurveyService(
                responseRepository,
                dismissalRepository,
                userRepository
        );
        user = new User("survey-user", "survey@test.com", Role.USER, Provider.KAKAO);
        lenient().when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    }

    @Test
    void statusReturnsAnsweredQuestionsAndReward() {
        when(responseRepository.findAllByUserUserId(7L)).thenReturn(List.of(
                new UsageSurveyResponse(
                        user,
                        UsageSurveyQuestion.INSTALL_REASON,
                        "SCHOOL_INFO"
                )
        ));

        var result = service.getStatus(7L);

        assertThat(result.answeredQuestionKeys())
                .containsExactly(UsageSurveyQuestion.INSTALL_REASON);
        assertThat(result.rewardPoint()).isEqualTo(10);
    }

    @Test
    void validResponseIsStoredAndMethodCarriesTenPointReward() throws Exception {
        var req = new SubmitUsageSurveyResponseReq(
                UsageSurveyQuestion.DESIRED_FEATURE,
                "STUDY_ROOM_LINK"
        );

        var result = service.submitResponse(7L, req);

        ArgumentCaptor<UsageSurveyResponse> captor =
                ArgumentCaptor.forClass(UsageSurveyResponse.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getQuestionKey())
                .isEqualTo(UsageSurveyQuestion.DESIRED_FEATURE);
        assertThat(captor.getValue().getOptionKey()).isEqualTo("STUDY_ROOM_LINK");
        assertThat(result.getRewardPoint()).isEqualTo(10);
        assertThat(UsageSurveyService.class
                .getMethod("submitResponse", Long.class, SubmitUsageSurveyResponseReq.class)
                .getAnnotation(UpdateScore.class)
                .addScore()).isEqualTo(10);
    }

    @Test
    void duplicateResponseIsRejectedBeforeReward() {
        when(responseRepository.existsByUserUserIdAndQuestionKey(
                7L,
                UsageSurveyQuestion.INSTALL_REASON
        )).thenReturn(true);

        assertThatThrownBy(() -> service.submitResponse(
                7L,
                new SubmitUsageSurveyResponseReq(
                        UsageSurveyQuestion.INSTALL_REASON,
                        "FACILITIES"
                )
        )).isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(ALREADY_ANSWERED_USAGE_SURVEY);
    }

    @Test
    void mismatchedOptionIsRejected() {
        assertThatThrownBy(() -> service.submitResponse(
                7L,
                new SubmitUsageSurveyResponseReq(
                        UsageSurveyQuestion.INSTALL_REASON,
                        "STUDY_ROOM_LINK"
                )
        )).isInstanceOf(GlobalException.class)
                .extracting("resultCode")
                .isEqualTo(INVALID_USAGE_SURVEY_OPTION);
    }

    @Test
    void laterDismissalIsStoredForExitMetrics() {
        service.recordDismissal(7L, new RecordUsageSurveyDismissalReq(
                UsageSurveyQuestion.DISAPPOINTMENT,
                UsageSurveyDismissReason.LATER
        ));

        ArgumentCaptor<UsageSurveyDismissal> captor =
                ArgumentCaptor.forClass(UsageSurveyDismissal.class);
        verify(dismissalRepository).save(captor.capture());
        assertThat(captor.getValue().getQuestionKey())
                .isEqualTo(UsageSurveyQuestion.DISAPPOINTMENT);
        assertThat(captor.getValue().getDismissReason())
                .isEqualTo(UsageSurveyDismissReason.LATER);
    }

    @Test
    void clientCloseAndMoreFeedbackReasonsAreStored() {
        service.recordDismissal(7L, new RecordUsageSurveyDismissalReq(
                UsageSurveyQuestion.INSTALL_REASON,
                UsageSurveyDismissReason.CLOSE
        ));
        service.recordDismissal(7L, new RecordUsageSurveyDismissalReq(
                UsageSurveyQuestion.RECENT_USE_REASON,
                UsageSurveyDismissReason.MORE_FEEDBACK
        ));

        ArgumentCaptor<UsageSurveyDismissal> captor =
                ArgumentCaptor.forClass(UsageSurveyDismissal.class);
        verify(dismissalRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(UsageSurveyDismissal::getDismissReason)
                .containsExactly(
                        UsageSurveyDismissReason.CLOSE,
                        UsageSurveyDismissReason.MORE_FEEDBACK
                );
    }
}
