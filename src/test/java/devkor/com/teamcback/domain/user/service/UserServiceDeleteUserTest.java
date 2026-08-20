package devkor.com.teamcback.domain.user.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.bookmark.repository.BookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.bookmark.repository.UserBookmarkLogRepository;
import devkor.com.teamcback.domain.character.repository.UserCharacterRepository;
import devkor.com.teamcback.domain.notification.service.PushInstallationService;
import devkor.com.teamcback.domain.suggestion.repository.SuggestionRepository;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.domain.user.validator.AppleValidator;
import devkor.com.teamcback.domain.user.validator.GoogleValidator;
import devkor.com.teamcback.domain.user.validator.KakaoValidator;
import devkor.com.teamcback.global.jwt.JwtUtil;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceDeleteUserTest {
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
    PushInstallationService pushInstallationService;
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

    @DisplayName("회원 탈퇴 시 캐릭터 보유 이력을 사용자 삭제 전에 정리 (FK 제약)")
    @Test
    void deleteUserCleansUpOwnedCharacters() {
        User user = new User("tester", "tester@test.com", Role.USER, Provider.KAKAO);
        ReflectionTestUtils.setField(user, "userId", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findByUser(user)).thenReturn(List.of());
        when(suggestionRepository.findByUser(user)).thenReturn(List.of());
        when(userBookmarkLogRepository.findByUser(user)).thenReturn(List.of());

        userService.deleteUser(1L);

        InOrder inOrder = Mockito.inOrder(userCharacterRepository, userRepository);
        inOrder.verify(userCharacterRepository).deleteAllByUser(user); // 소유 이력 먼저
        inOrder.verify(userRepository).delete(user);                   // 그 다음 사용자 삭제
    }
}
