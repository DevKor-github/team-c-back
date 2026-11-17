package devkor.com.teamcback.domain.apistatistics.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiStatisticsResponse {
    private String environment;
    private LocalDate collectionStartDate;
    private long todayCallCount;
    private long yesterdayCallCount;
    private long last7DaysCallCount;
    private long last30DaysCallCount;
    private List<DailyCountResponse> dailyCounts;
    private List<MonthlyCountResponse> monthlyCounts;
    private List<EndpointCountResponse> topEndpoints;

    @Getter
    @AllArgsConstructor
    public static class DailyCountResponse {
        private LocalDate date;
        private long callCount;
    }

    @Getter
    @AllArgsConstructor
    public static class MonthlyCountResponse {
        private String month;
        private long callCount;
    }

    @Getter
    @AllArgsConstructor
    public static class EndpointCountResponse {
        private String endpoint;
        private String method;
        private long callCount;
    }
}
