package devkor.com.teamcback.global.jwt;

import static devkor.com.teamcback.global.jwt.JwtUtil.ACCESS_TOKEN_HEADER;
import static devkor.com.teamcback.global.response.ResultCode.*;

import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.redis.RedisUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j(topic = "JWT validation & authorization")
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final List<RequestMatcher> whiteList =
        List.of(
            new AntPathRequestMatcher("/api/migration"),
            new AntPathRequestMatcher("/api/koyeon/**"),
            new AntPathRequestMatcher("/api/routes/**"),
            new AntPathRequestMatcher("/api/search", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/search/buildings", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/search/buildings/**/facilities/**", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/search/buildings/**/floor/**", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/search/facilities", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/search/place/**/mask", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/users/login/**"));

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String accessToken = jwtUtil.getAccessTokenFromHeader(request);
        log.info("Access Token: {}", accessToken);

        // access token 비어있으면 인증 미처리
        if (!StringUtils.hasText(accessToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        // access token 검증
        switch(jwtUtil.validateToken(accessToken)) {
            case VALID -> setAuthentication(jwtUtil.getUserIdFromToken(accessToken));
            case INVALID -> throw new GlobalException(INVALID_TOKEN);
            case EXPIRED -> authenticateRefreshToken(request, response);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 현재 URL 이 화이트 리스트에 존재하는지 체크
        return whiteList.stream().anyMatch(whitePath -> whitePath.matches(request));
    }

    /**
     * 인증 처리 (인증 객체를 생성하여 context에 설정)
     */
    private void setAuthentication(String userId) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    /**
     * Refresh token 검증 (access token이 만료된 경우)
     */
    private void authenticateRefreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtUtil.getRefreshTokenFromHeader(request);
//        log.info("Refresh Token: {}", refreshToken);

        if(refreshToken == null) throw new GlobalException(REFRESH_TOKEN_REQUIRED); // refresh token 요청

        // 로그아웃된 refresh token 인지 확인
        if(redisUtil.hasKey(refreshToken)) {
            log.info("로그아웃 된 Refresh Token");
            throw new GlobalException(LOG_IN_REQUIRED);
        }

        switch(jwtUtil.validateToken(refreshToken)) {
            case VALID -> renewAccessToken(response, refreshToken);
            case INVALID ->  throw new GlobalException(INVALID_TOKEN);
            case EXPIRED -> throw new GlobalException(REFRESH_TOKEN_EXPIRED);
        }
    }

    /**
     * Access token 재발급 후 요청 처리
     */
    private void renewAccessToken(HttpServletResponse response, String refreshToken) {
        String userId = jwtUtil.getUserIdFromToken(refreshToken);
        String role = jwtUtil.getRoleFromToken(refreshToken);

        String accessToken = jwtUtil.createAccessToken(userId, role);
        response.addHeader(ACCESS_TOKEN_HEADER, accessToken);

        setAuthentication(userId);
    }
}
