package devkor.com.teamcback.domain.user.service;

import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.entity.Color;
import devkor.com.teamcback.domain.bookmark.repository.BookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.bookmark.repository.UserBookmarkLogRepository;
import devkor.com.teamcback.domain.suggestion.entity.Suggestion;
import devkor.com.teamcback.domain.suggestion.repository.SuggestionRepository;
import devkor.com.teamcback.domain.user.dto.request.BypassLoginReq;
import devkor.com.teamcback.domain.user.dto.request.LoginUserReq;
import devkor.com.teamcback.domain.user.dto.response.BypassLoginRes;
import devkor.com.teamcback.domain.user.dto.response.DeleteUserRes;
import devkor.com.teamcback.domain.user.dto.response.GetUserInfoRes;
import devkor.com.teamcback.domain.user.dto.response.LoginUserRes;
import devkor.com.teamcback.domain.user.dto.response.ModifyUsernameRes;
import devkor.com.teamcback.domain.user.dto.response.TempLoginRes;
import devkor.com.teamcback.domain.user.entity.Level;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.domain.user.validator.AppleValidator;
import devkor.com.teamcback.domain.user.validator.GoogleValidator;
import devkor.com.teamcback.domain.user.validator.KakaoValidator;
import devkor.com.teamcback.global.exception.GlobalException;
import devkor.com.teamcback.global.jwt.JwtUtil;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserBookmarkLogRepository userBookmarkLogRepository;
    private final SuggestionRepository suggestionRepository;
    private final JwtUtil jwtUtil;
    private final KakaoValidator kakaoValidator;
    private final GoogleValidator googleValidator;
    private final AppleValidator appleValidator;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_NAME = "호랑이";
    private static final String DEFAULT_CATEGORY = "내 장소";
    private static final Color DEFAULT_COLOR = Color.RED;

    @Value("${jwt.admin.token}")
    private String adminToken;

    /**
     * 마이페이지 정보 조회
     */
    @Transactional(readOnly = true)
    public GetUserInfoRes getUserInfo(Long userId) {
        User user = findUser(userId);
        Level level = getLevel(user.getScore());
        Level nextLevel = level.getNextLevel();
        Long remainScoreToNextLevel =  nextLevel == null ? 0 : nextLevel.getMinScore() - user.getScore();
        int percent = 100;
        if(nextLevel != null) {
            percent = (int) (100 * (user.getScore() - level.getMinScore()) / (nextLevel.getMinScore() - level.getMinScore()));
        }
        return new GetUserInfoRes(user, categoryRepository.countAllByUser(user), level.getLevelNumber(), remainScoreToNextLevel, percent);
    }

    /**
     * 로그인 (안드로이드 배포 수정 후 삭제)
     */
    @Transactional
    public TempLoginRes login(LoginUserReq loginUserReq) {
        User user = userRepository.findByEmailAndProvider(loginUserReq.getEmail(), loginUserReq.getProvider()); // 이메일이 같더라도 소셜이 다르면 다른 사용자 취급
        if(user == null) { // 회원이 없으면 회원가입
            String username = makeRandomName();
            user = userRepository.save(new User(username, loginUserReq.getEmail(), Role.USER, loginUserReq.getProvider()));

            // 기본 카테고리 저장
            Category category = new Category(DEFAULT_CATEGORY, DEFAULT_COLOR, user);
            categoryRepository.save(category);
        }

        return new TempLoginRes(jwtUtil.createAccessToken(user.getUserId().toString(), user.getRole().getAuthority()), jwtUtil.createRefreshToken(user.getUserId().toString(), user.getRole().getAuthority()));
    }

    /**
     * 로그인
     */
    @Transactional
    public LoginUserRes releaseLogin(LoginUserReq loginUserReq) {
        String email = loginUserReq.getEmail();
        if(!loginUserReq.getToken().equals(adminToken)) { // 관리자용 토큰 입력 시 검증 과정 생략
            email = validateToken(loginUserReq.getProvider(), loginUserReq.getToken());
        }

        User user = userRepository.findByEmailAndProvider(email, loginUserReq.getProvider()); // 이메일이 같더라도 소셜이 다르면 다른 사용자 취급
        if(user == null) { // 회원이 없으면 회원가입
            String username = makeRandomName();
            user = userRepository.save(new User(username, email, Role.USER, loginUserReq.getProvider()));

            // 기본 카테고리 저장
            Category category = new Category(DEFAULT_CATEGORY, DEFAULT_COLOR, user);
            categoryRepository.save(category);
        }

        String rawCode = UUID.randomUUID().toString();
        user.setCode(passwordEncoder.encode(rawCode));

        return new LoginUserRes(jwtUtil.createAccessToken(user.getUserId().toString(), user.getRole().getAuthority()), jwtUtil.createRefreshToken(user.getUserId().toString(), user.getRole().getAuthority()), rawCode);
    }

    private String validateToken(Provider provider, String token) {
        return switch (provider) {
            case KAKAO -> kakaoValidator.validateToken(token);
            case GOOGLE -> googleValidator.validateToken(token);
            case APPLE -> appleValidator.validateToken(token);
            default -> throw new GlobalException(INVALID_INPUT);
        };
    }

    /**
     * 자동 로그인
     */
    @Transactional
    public BypassLoginRes bypassLogin(BypassLoginReq bypassLoginReq) {
        User user = userRepository.findByEmailAndProvider(bypassLoginReq.getEmail(), bypassLoginReq.getProvider());

        validateUser(user, bypassLoginReq.getCode());

        return new BypassLoginRes(jwtUtil.createAccessToken(user.getUserId().toString(), user.getRole().getAuthority()), jwtUtil.createRefreshToken(user.getUserId().toString(), user.getRole().getAuthority()));
    }

    private void validateUser(User user, String code) {
        if(user == null) throw new GlobalException(NOT_FOUND_USER);

        if(!passwordEncoder.matches(code, user.getCode())) throw new GlobalException(INVALID_INPUT);
    }

    /**
     * 사용자 별명 수정
     */
    @Transactional
    public ModifyUsernameRes modifyUsername(Long userId, String username) {
        checkInvalidUsername(username);

        // 사용자명 사용 가능 여부 확인
        User user = findUser(userId);
        checkUsernameAvailability(user, username);

        user.updateUsername(username);

        return new ModifyUsernameRes();
    }

    /**
     * 사용자 회원 탈퇴
     */
    @Transactional
    public DeleteUserRes deleteUser(Long userId) {
        User user = findUser(userId);

        List<Category> categoryList = categoryRepository.findByUser(user);

        for(Category category : categoryList) {
            // 각 북마크가 다른 카테고리와 연결되어 있지 않은지 확인 후 삭제
            category.getCategoryBookmarkList().forEach(categoryBookmark -> {
                Bookmark bookmark = categoryBookmark.getBookmark();
                if (bookmark.getCategoryBookmarkList().size() == 1) {
                    bookmarkRepository.delete(bookmark);
                }
            });

            categoryRepository.delete(category);
        }

        // 건의 익명 처리
        List<Suggestion> suggestions = suggestionRepository.findByUser(user);
        for(Suggestion suggestion : suggestions) {
            suggestion.setUser(null);
        }

        userBookmarkLogRepository.deleteAll(userBookmarkLogRepository.findByUser(user));
//        suggestionRepository.deleteAll(suggestionRepository.findByUser(user));
        userRepository.delete(user);

        return new DeleteUserRes();
    }

    private void checkInvalidUsername(String username) {
        // username이 입력되지 않은 경우
        if(username.isEmpty()) {
            throw new GlobalException(EMPTY_USERNAME);
        }
    }

    private void checkUsernameAvailability(User user, String username) {
        // 본인이 현재 사용 중인 닉네임과 동일한 경우
        if(username.equals(user.getUsername())) {
            throw new GlobalException(USERNAME_IN_USE);
        }

        // 다른 사용자가 해당 별명을 사용 중인 경우
        if(userRepository.existsByUsernameAndUserIdNot(username, user.getUserId())) {
            throw new GlobalException(DUPLICATED_USERNAME);
        }
    }

    private Level getLevel(Long score) {
        // score >= minScore 인 경우 중 가장 높은 레벨 반환
        return Arrays.stream(Level.values())
            .filter(lv -> score >= lv.getMinScore())
            .max(Comparator.comparingInt(Level::getMinScore))
            .orElse(Level.LEVEL1);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }

    private String makeRandomName() {
        String randomName;
        do {
            randomName = DEFAULT_NAME + UUID.randomUUID().toString().substring(0, 6);
        } while (userRepository.existsByUsername(randomName));
        return randomName;
    }
}
