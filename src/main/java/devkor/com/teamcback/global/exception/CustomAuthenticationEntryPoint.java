package devkor.com.teamcback.global.exception;

import static devkor.com.teamcback.global.response.ResultCode.UNAUTHORIZED;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException authException) {
        ObjectMapper objectMapper = new ObjectMapper();
        response.setStatus(UNAUTHORIZED.getStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            response.getWriter().write(objectMapper.writeValueAsString(
                new CommonResponse<>(UNAUTHORIZED)));
        } catch (IOException e) {
            throw new GlobalException(ResultCode.SYSTEM_ERROR);
        }
    }
}
