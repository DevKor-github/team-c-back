package devkor.com.teamcback.global.logging;

import devkor.com.teamcback.global.security.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            MDC.put("traceId", UUID.randomUUID().toString());
            MDC.put("endpoint", request.getRequestURI());

            String userId = getUserIdFromToken();
            if (userId != null) MDC.put("userId", userId);


            LoggerFactory.getLogger("REQUEST_LOG").info("요청 시작");
            filterChain.doFilter(request, response);
        } finally {
            long latency = System.currentTimeMillis() - startTime;
            MDC.put("status", String.valueOf(response.getStatus()));
            MDC.put("latencyMs", String.valueOf(latency));
            LoggerFactory.getLogger("REQUEST_LOG").info("요청 완료");
            MDC.clear();
        }
    }

    private String getUserIdFromToken() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return String.valueOf(userDetails.getUser().getUserId());
        }
        return "GUEST";
    }
}
