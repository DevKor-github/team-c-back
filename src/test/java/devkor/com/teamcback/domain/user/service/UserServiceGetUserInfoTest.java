package devkor.com.teamcback.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.bookmark.repository.BookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.bookmark.repository.UserBookmarkLogRepository;
import devkor.com.teamcback.domain.character.repository.UserCharacterRepository;
import devkor.com.teamcback.domain.suggestion.repository.SuggestionRepository;
import devkor.com.teamcback.domain.user.dto.response.GetUserInfoRes;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.domain.user.validator.AppleValidator;
import devkor.com.teamcback.domain.user.validator.GoogleValidator;
import devkor.com.teamcback.domain.user.validator.KakaoValidator;
import devkor.com.teamcback.global.jwt.JwtUtil;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceGetUserInfoTest {
    @InjectMocks
    UserService userService;

    @Mock
    UserRepository userRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    BookmarkRepository bookmarkRepository;
    @Mock
    UserBookmarkLogRepository userBookmarkLogRepository;
    @Mock
    SuggestionRepository suggestionRepository;
    @Mock
    UserCharacterRepository userCharacterRepository;
    @Mock
    JwtUtil jwtUtil;
    @Mock
    KakaoValidator kakaoValidator;
    @Mock
    GoogleValidator googleValidator;
    @Mock
    AppleValidator appleValidator;
    @Mock
    PasswordEncoder passwordEncoder;

    @DisplayName("마이페이지: 영속화된 level 컬럼 기반 응답 + isUpgraded 리셋")
    @Test
    void getUserInfo() {
        User user = new User("tester", "tester@test.com", Role.USER, Provider.KAKAO);
        ReflectionTestUtils.setField(user, "userId", 1L);
        user.updateScore(10L, true); // LEVEL2, 레벨업 직후 상태

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.countAllByUser(user)).thenReturn(1L);

        GetUserInfoRes res = userService.getUserInfo(1L);

        assertEquals(2, res.getLevel());
        assertEquals(10L, res.getScore());
        assertEquals(0L, res.getPoint()); // updateScore는 포인트를 건드리지 않음 (적립은 aspect에서)
        assertEquals(10L, res.getRemainScoreToNextLevel()); // LEVEL3 시작(20) - 10
        assertEquals(33, res.getPercent()); // 100 * (10-5) / (20-5)
        assertTrue(res.isUpgraded());
        assertFalse(user.isUpgraded()); // 조회 후 리셋
    }
}
