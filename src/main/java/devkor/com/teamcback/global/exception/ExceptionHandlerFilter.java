package devkor.com.teamcback.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.response.ResultCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class ExceptionHandlerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } catch (GlobalException e) {
            setErrorResponse(response, e.getResultCode());
        }
    }

    private void setErrorResponse(HttpServletResponse response, ResultCode resultCode) {
        ObjectMapper objectMapper = new ObjectMapper();
        response.setStatus(resultCode.getStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            response.getWriter().write(objectMapper.writeValueAsString(
                new CommonResponse<>(resultCode)));
        } catch (IOException e) {
            throw new GlobalException(ResultCode.SYSTEM_ERROR);
        }
    }

}
