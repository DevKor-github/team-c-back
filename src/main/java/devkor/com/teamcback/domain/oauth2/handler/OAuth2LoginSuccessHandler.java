package devkor.com.teamcback.domain.oauth2.handler;

import static devkor.com.teamcback.global.jwt.JwtUtil.ACCESS_TOKEN_HEADER;
import static devkor.com.teamcback.global.jwt.JwtUtil.REFRESH_TOKEN_HEADER;

import devkor.com.teamcback.global.jwt.JwtUtil;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;
    private static final String REDIRECT_URL = "myapp://callback";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String email = userDetails.getUser().getEmail();

        List<? extends GrantedAuthority> list = new ArrayList<>(authentication.getAuthorities());
        String role = list.get(0).getAuthority();

        String accessToken = jwtUtil.createAccessToken(email, String.valueOf(role));
        String refreshToken = jwtUtil.createRefreshToken(email, String.valueOf(role));

        // FE URL로 리디렉션
        String redirectUrl = REDIRECT_URL + "?accessToken=" + accessToken + "&refreshToken=" + refreshToken;
        response.sendRedirect(redirectUrl);
//
//        response.setHeader(ACCESS_TOKEN_HEADER, accessToken);
//        response.setHeader(REFRESH_TOKEN_HEADER, refreshToken);
    }
}
