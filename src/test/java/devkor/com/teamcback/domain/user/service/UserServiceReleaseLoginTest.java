package devkor.com.teamcback.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.bookmark.repository.BookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.bookmark.repository.UserBookmarkLogRepository;
import devkor.com.teamcback.domain.character.repository.UserCharacterRepository;
import devkor.com.teamcback.domain.notification.service.PushInstallationService;
import devkor.com.teamcback.domain.suggestion.repository.SuggestionRepository;
import devkor.com.teamcback.domain.user.dto.request.LoginUserReq;
import devkor.com.teamcback.domain.user.dto.response.LoginUserRes;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.domain.user.validator.AppleValidator;
import devkor.com.teamcback.domain.user.validator.GoogleValidator;
import devkor.com.teamcback.domain.user.validator.KakaoValidator;
import devkor.com.teamcback.global.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceReleaseLoginTest {
    @InjectMocks
    UserService userService;

    @Mock UserRepository userRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock BookmarkRepository bookmarkRepository;
    @Mock UserBookmarkLogRepository userBookmarkLogRepository;
    @Mock SuggestionRepository suggestionRepository;
    @Mock UserCharacterRepository userCharacterRepository;
    @Mock JwtUtil jwtUtil;
    @Mock KakaoValidator kakaoValidator;
    @Mock GoogleValidator googleValidator;
    @Mock AppleValidator appleValidator;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PushInstallationService pushInstallationService;

    LoginUserReq request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "adminToken", "admin-token");
        request = new LoginUserReq();
        ReflectionTestUtils.setField(request, "provider", Provider.KAKAO);
        ReflectionTestUtils.setField(request, "email", "untrusted-client@example.com");
        ReflectionTestUtils.setField(request, "token", "kakao-id-token");
    }

    @Test
    void returnsLoginKeyExtractedFromVerifiedToken() {
        User user = new User("member", "verified@example.com", Role.USER, Provider.KAKAO);
        ReflectionTestUtils.setField(user, "userId", 3L);
        when(kakaoValidator.validateToken("kakao-id-token")).thenReturn("verified@example.com");
        when(userRepository.findByEmailAndProvider("verified@example.com", Provider.KAKAO)).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-code");
        when(jwtUtil.createAccessToken("3", "ROLE_USER")).thenReturn("access");
        when(jwtUtil.createRefreshToken("3", "ROLE_USER")).thenReturn("refresh");

        LoginUserRes result = userService.releaseLogin(request);

        assertEquals("verified@example.com", result.getLoginKey());
        assertEquals("access", result.getAccessToken());
        assertEquals("refresh", result.getRefreshToken());
        verify(userRepository).findByEmailAndProvider("verified@example.com", Provider.KAKAO);
    }
}
