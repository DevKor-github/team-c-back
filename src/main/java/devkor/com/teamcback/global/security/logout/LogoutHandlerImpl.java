package devkor.com.teamcback.global.security.logout;

import devkor.com.teamcback.global.jwt.JwtUtil;
import devkor.com.teamcback.global.redis.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Slf4j(topic = "logout")
@Component
@RequiredArgsConstructor
public class LogoutHandlerImpl implements LogoutHandler {
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private static final String LOGOUT_VALUE = "logout";

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) {

        String refreshToken = jwtUtil.getRefreshTokenFromHeader(request);
        log.info("refreshToken: {}", refreshToken);

        // 블랙리스트 처리: Redis에 저장
        redisUtil.set(refreshToken, LOGOUT_VALUE, jwtUtil.getExpiration(refreshToken));
    }
}
