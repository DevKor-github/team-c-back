package devkor.com.teamcback.global.security.logout;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.global.jwt.JwtUtil;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.logout.dto.UserLogoutRes;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {

        SecurityContextHolder.clearContext();

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.getWriter().write(new ObjectMapper().writeValueAsString(CommonResponse.success(new UserLogoutRes())));
    }

    private void deleteCookie(HttpServletResponse response, String header) {
        Cookie cookie = new Cookie(header, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
