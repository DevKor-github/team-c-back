package devkor.com.teamcback.domain.apistatistics.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiEndpointTrendResponse {
    private String endpoint;
    private String method;
    private LocalDate collectionStartDate;
    private long last30DaysCallCount;
    private List<ApiStatisticsResponse.DailyCountResponse> dailyCounts;
    private List<ApiStatisticsResponse.MonthlyCountResponse> monthlyCounts;
}
