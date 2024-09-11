package devkor.com.teamcback.domain.user.service;

import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.user.dto.request.LoginUserReq;
import devkor.com.teamcback.domain.user.dto.response.GetUserInfoRes;
import devkor.com.teamcback.domain.user.dto.response.LoginUserRes;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import devkor.com.teamcback.global.jwt.JwtUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final JwtUtil jwtUtil;
    private static final String DEFAULT_NAME = "호랑이";

    /**
     * 마이페이지 정보 조회
     */
    @Transactional(readOnly = true)
    public GetUserInfoRes getUserInfo(Long userId) {
        User user = findUser(userId);
        return new GetUserInfoRes(user, categoryRepository.countAllByUser(user));
    }

    /**
     * 로그인
     */
    @Transactional
    public LoginUserRes login(LoginUserReq loginUserReq) {
        // TODO: 소셜 검증 - 사용자가 소셜 서버의 사용자인지 확인
        log.info("social-token: {}", loginUserReq.getToken());

        User user = userRepository.findByEmailAndProvider(loginUserReq.getEmail(), loginUserReq.getProvider()); // 이메일이 같더라도 소셜이 다르면 다른 사용자 취급
        if(user == null) { // 회원이 없으면 회원가입
            String username = makeRandomName();
            user = userRepository.save(new User(username, loginUserReq.getEmail(), Role.USER, loginUserReq.getProvider()));
        }

        return new LoginUserRes(jwtUtil.createAccessToken(user.getUsername(), user.getRole().getAuthority()), jwtUtil.createRefreshToken(user.getUsername(), user.getRole().getAuthority()));
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
