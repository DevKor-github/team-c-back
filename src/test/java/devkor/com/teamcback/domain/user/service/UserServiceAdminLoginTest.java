package devkor.com.teamcback.domain.user.service;

import static devkor.com.teamcback.global.response.ResultCode.FORBIDDEN;
import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.bookmark.repository.BookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.bookmark.repository.UserBookmarkLogRepository;
import devkor.com.teamcback.domain.character.repository.UserCharacterRepository;
import devkor.com.teamcback.domain.notification.service.PushInstallationService;
import devkor.com.teamcback.domain.suggestion.repository.SuggestionRepository;
import devkor.com.teamcback.domain.user.dto.request.AdminLoginReq;
import devkor.com.teamcback.domain.user.dto.response.AdminLoginRes;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.domain.user.validator.AppleValidator;
import devkor.com.teamcback.domain.user.validator.GoogleValidator;
import devkor.com.teamcback.domain.user.validator.KakaoValidator;
import devkor.com.teamcback.global.exception.exception.GlobalException;
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
class UserServiceAdminLoginTest {
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

    AdminLoginReq request;

    @BeforeEach
    void setUp() {
        request = new AdminLoginReq();
        ReflectionTestUtils.setField(request, "provider", Provider.GOOGLE);
        ReflectionTestUtils.setField(request, "token", "google-id-token");
    }

    @Test
    void issuesTokensOnlyForExistingAdmin() {
        User admin = new User("operator", "admin@example.com", Role.ADMIN, Provider.GOOGLE);
        ReflectionTestUtils.setField(admin, "userId", 7L);
        when(googleValidator.validateToken("google-id-token")).thenReturn("admin@example.com");
        when(userRepository.findByEmailAndProvider("admin@example.com", Provider.GOOGLE)).thenReturn(admin);
        when(jwtUtil.createAccessToken("7", "ROLE_ADMIN")).thenReturn("access");
        when(jwtUtil.createRefreshToken("7", "ROLE_ADMIN")).thenReturn("refresh");

        AdminLoginRes result = userService.adminLogin(request);

        assertEquals("access", result.getAccessToken());
        assertEquals("refresh", result.getRefreshToken());
        assertEquals(7L, result.getUserId());
        assertEquals("admin@example.com", result.getEmail());
    }

    @Test
    void rejectsNonAdminWithoutCreatingUser() {
        User user = new User("user", "user@example.com", Role.USER, Provider.GOOGLE);
        when(googleValidator.validateToken("google-id-token")).thenReturn("user@example.com");
        when(userRepository.findByEmailAndProvider("user@example.com", Provider.GOOGLE)).thenReturn(user);

        GlobalException exception = assertThrows(GlobalException.class, () -> userService.adminLogin(request));

        assertEquals(FORBIDDEN, exception.getResultCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsUnsupportedProviderBeforeTokenValidation() {
        ReflectionTestUtils.setField(request, "provider", Provider.APPLE);

        GlobalException exception = assertThrows(GlobalException.class, () -> userService.adminLogin(request));

        assertEquals(INVALID_INPUT, exception.getResultCode());
        verify(appleValidator, never()).validateToken("google-id-token");
    }
}
