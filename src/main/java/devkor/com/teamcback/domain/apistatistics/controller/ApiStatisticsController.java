package devkor.com.teamcback.domain.apistatistics.controller;

import devkor.com.teamcback.domain.apistatistics.dto.response.ApiEndpointResponse;
import devkor.com.teamcback.domain.apistatistics.dto.response.ApiEndpointTrendResponse;
import devkor.com.teamcback.domain.apistatistics.dto.response.ApiStatisticsResponse;
import devkor.com.teamcback.domain.apistatistics.service.ApiStatisticsService;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.response.ResultCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/api-statistics")
public class ApiStatisticsController {

    private final ApiStatisticsService apiStatisticsService;

    @GetMapping
    public CommonResponse<ApiStatisticsResponse> getStatistics() {
        return CommonResponse.success(apiStatisticsService.getStatistics());
    }

    @GetMapping("/endpoints")
    public CommonResponse<?> getEndpoints(
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) String method) {
        if (endpoint == null && method == null) {
            List<ApiEndpointResponse> endpoints = apiStatisticsService.getEndpoints();
            return CommonResponse.success(endpoints);
        }
        if (endpoint == null || method == null) {
            throw new GlobalException(ResultCode.INVALID_INPUT);
        }
        ApiEndpointTrendResponse trend = apiStatisticsService.getEndpointTrend(endpoint, method);
        return CommonResponse.success(trend);
    }
}
