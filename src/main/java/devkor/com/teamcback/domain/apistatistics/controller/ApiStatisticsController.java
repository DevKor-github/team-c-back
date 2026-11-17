package devkor.com.teamcback.domain.apistatistics.controller;

import devkor.com.teamcback.domain.apistatistics.dto.response.ApiStatisticsResponse;
import devkor.com.teamcback.domain.apistatistics.service.ApiStatisticsService;
import devkor.com.teamcback.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
