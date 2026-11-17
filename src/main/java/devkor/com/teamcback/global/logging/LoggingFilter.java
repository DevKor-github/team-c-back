package devkor.com.teamcback.global.logging;

import devkor.com.teamcback.domain.apistatistics.service.ApiStatisticsService;
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
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private final ApiStatisticsService apiStatisticsService;

    public LoggingFilter(ApiStatisticsService apiStatisticsService) {
        this.apiStatisticsService = apiStatisticsService;
    }

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
            recordApiStatistics(request);
            MDC.clear();
        }
    }

    private void recordApiStatistics(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (isExcluded(uri)) return;

        String endpoint = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (endpoint == null || endpoint.isBlank()) endpoint = "UNMATCHED_API";

        try {
            apiStatisticsService.record(
                    java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")),
                    endpoint,
                    request.getMethod()
            );
        } catch (Exception e) {
            log.warn("API statistics recording failed. method={}, endpoint={}", request.getMethod(), endpoint, e);
        }
    }

    private boolean isExcluded(String uri) {
        return !uri.startsWith("/api/")
                || uri.equals("/actuator")
                || uri.startsWith("/actuator/")
                || uri.equals("/swagger-ui")
                || uri.startsWith("/swagger-ui/")
                || uri.equals("/v3/api-docs")
                || uri.startsWith("/v3/api-docs/")
                || uri.startsWith("/api/admin/api-statistics/")
                || uri.equals("/api/admin/api-statistics");
    }

    private String getUserIdFromToken() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return String.valueOf(userDetails.getUser().getUserId());
        }
        return "GUEST";
    }
}
