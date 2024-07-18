package devkor.com.teamcback.global.exception;

import static devkor.com.teamcback.global.response.ResultCode.UNAUTHORIZED;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.response.ResultCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
        AccessDeniedException accessDeniedException) {
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
