package devkor.com.teamcback.domain.user.service;

import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.oauth2.dto.OAuth2UserInfo;
import devkor.com.teamcback.domain.user.dto.request.LoginUserReq;
import devkor.com.teamcback.domain.user.dto.response.GetUserInfoRes;
import devkor.com.teamcback.domain.user.dto.response.LoginUserRes;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import devkor.com.teamcback.global.jwt.JwtUtil;
import devkor.com.teamcback.global.response.CommonResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public GetUserInfoRes getUserInfo(Long userId) {
        User user = findUser(userId);
        return new GetUserInfoRes(user, categoryRepository.countAllByUser(user));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }

    /**
     * 로그인
     */
    public LoginUserRes login(LoginUserReq loginUserReq) {
        String username = makeRandomName();
        User user = userRepository.findByEmail(loginUserReq.getEmail());
        // TODO: 이메일 예외 처리
        if(user == null) {
            user = userRepository.save(new User(username, loginUserReq.getEmail(), Role.USER, loginUserReq.getProvider()));
        }

        return new LoginUserRes(jwtUtil.createAccessToken(user.getEmail(), user.getRole().getAuthority()), jwtUtil.createRefreshToken(user.getEmail(), user.getRole().getAuthority()));
    }

    private String makeRandomName() {
        String randomName;
        do {
            randomName = DEFAULT_NAME + UUID.randomUUID().toString().substring(0, 6);
        } while (userRepository.existsByUsername(randomName));
        return randomName;
    }
}
