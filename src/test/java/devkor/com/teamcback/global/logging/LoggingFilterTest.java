package devkor.com.teamcback.global.logging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import devkor.com.teamcback.domain.apistatistics.service.ApiStatisticsService;
import jakarta.servlet.FilterChain;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerMapping;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingFilterTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private ApiStatisticsService apiStatisticsService;

    @Test
    void recordsOneCallAtRequestCompletionUsingMvcPattern() throws Exception {
        var filter = new LoggingFilter(apiStatisticsService);
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/api/users/123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
                .thenReturn("/api/users/{userId}");

        filter.doFilterInternal(request, response, chain);

        verify(apiStatisticsService).record(
                eq(LocalDate.now(SEOUL_ZONE)), eq("/api/users/{userId}"), eq("GET"));
    }

    @Test
    void excludesStatisticsEndpoint() throws Exception {
        var filter = new LoggingFilter(apiStatisticsService);
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/api/admin/api-statistics");

        filter.doFilterInternal(request, response, chain);

        verify(apiStatisticsService, never()).record(any(), any(), any());
    }

    @Test
    void statisticsFailureDoesNotFailOriginalRequest() throws Exception {
        var filter = new LoggingFilter(apiStatisticsService);
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
                .thenReturn("/api/users");
        doThrow(new RuntimeException("database unavailable"))
                .when(apiStatisticsService).record(any(), any(), any());

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
