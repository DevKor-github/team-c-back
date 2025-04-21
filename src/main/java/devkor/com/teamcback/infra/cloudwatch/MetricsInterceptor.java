package devkor.com.teamcback.infra.cloudwatch;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MetricsInterceptor implements HandlerInterceptor {

    private final MetricsService metricsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod) {
            String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if(shouldTrack(pattern)) {
                metricsService.recordApiRequest(pattern);
            }
            return true;
        }
        return true;
    }

    private boolean shouldTrack(String uri) {
        return List.of(
                "/api/bookmarks",
                "/api/categories",
                "/api/routes",
                "/api/users/login",
                "/api/users/login/release",
                "/api/users/mypage",
                "/api/search",
                "/api/search/buildings",
                "/api/search/buildings/{buildingId}",
                "/api/search/buildings/{buildingId}/facilities",
                "/api/search/facilities",
                "/api/search/place/{placeId}"
        ).contains(uri);
    }
}


