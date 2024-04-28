package devkor.com.teamcback.domain.oauth2.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.domain.oauth2.dto.OAuth2LoginRes;
import devkor.com.teamcback.global.exception.GlobalException;
import devkor.com.teamcback.global.jwt.JwtUtil;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.response.ResultCode;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String email = userDetails.getUser().getEmail();

        List<? extends GrantedAuthority> list = new ArrayList<>(authentication.getAuthorities());
        String role = list.get(0).getAuthority();

        String accessToken = jwtUtil.createAccessToken(email, String.valueOf(role));
        String refreshToken = jwtUtil.createRefreshToken(email, String.valueOf(role));

        addCookie(accessToken, JwtUtil.ACCESS_TOKEN_HEADER, response);
        addCookie(refreshToken, JwtUtil.REFRESH_TOKEN_HEADER, response);

        setResponse(response, new OAuth2LoginRes());
    }

    private void addCookie(String cookieValue, String header, HttpServletResponse res) {
        Cookie cookie = new Cookie(header, cookieValue); // Name-Value
        cookie.setPath("/");
        cookie.setMaxAge(2 * 60 * 60); //쿠키 유효 기간(s) 2시간
        res.addCookie(cookie);
    }

    private void setResponse(HttpServletResponse response, Object data) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
            CommonResponse.success(data)));
    }
}
