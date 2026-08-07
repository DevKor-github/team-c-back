package devkor.com.teamcback.global.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import devkor.com.teamcback.domain.common.repository.FileRepository;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.review.repository.ReviewRepository;
import devkor.com.teamcback.domain.suggestion.repository.SuggestionRepository;
import devkor.com.teamcback.domain.user.entity.Level;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.domain.vote.repository.VoteRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateScoreLevelTest {
    @InjectMocks
    UpdateScoreAspect updateScoreAspect;

    @Mock
    UserRepository userRepository;
    @Mock
    SuggestionRepository suggestionRepository;
    @Mock
    VoteRecordRepository voteRecordRepository;
    @Mock
    ReviewRepository reviewRepository;
    @Mock
    PlaceRepository placeRepository;
    @Mock
    FileRepository fileRepository;

    @DisplayName("점수 적립 시 레벨업 판정과 level 컬럼 갱신 + 포인트 동일 적립")
    @Test
    void increaseScoreUpdatesLevel() {
        User user = new User("tester", "tester@test.com", Role.USER, Provider.KAKAO);

        // 건의 작성 +10점 → LEVEL1(0) → LEVEL2(5~) 레벨업, 포인트도 +10
        updateScoreAspect.increaseScore(user, 10);
        assertEquals(10L, user.getScore());
        assertEquals(10L, user.getPoint());
        assertEquals(Level.LEVEL2, user.getLevel());
        assertTrue(user.isUpgraded());

        // 같은 레벨 내 적립은 레벨업 아님
        updateScoreAspect.increaseScore(user, 5);
        assertEquals(15L, user.getScore());
        assertEquals(15L, user.getPoint());
        assertEquals(Level.LEVEL2, user.getLevel());
        assertFalse(user.isUpgraded());

        // 리뷰 최대 점수(+13)로 임계값을 건너뛰어도 레벨과 컬럼이 일치
        updateScoreAspect.increaseScore(user, 13);
        assertEquals(28L, user.getScore());
        assertEquals(28L, user.getPoint());
        assertEquals(Level.LEVEL3, user.getLevel());
        assertTrue(user.isUpgraded());
    }
}
