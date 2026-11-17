package devkor.com.teamcback.domain.apistatistics.service;

import devkor.com.teamcback.domain.apistatistics.dto.response.ApiStatisticsResponse;
import devkor.com.teamcback.domain.apistatistics.dto.response.ApiStatisticsResponse.DailyCountResponse;
import devkor.com.teamcback.domain.apistatistics.dto.response.ApiStatisticsResponse.EndpointCountResponse;
import devkor.com.teamcback.domain.apistatistics.dto.response.ApiStatisticsResponse.MonthlyCountResponse;
import devkor.com.teamcback.domain.apistatistics.repository.ApiDailyStatisticRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiStatisticsService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final ApiDailyStatisticRepository repository;

    @Value("${metrics.environment}")
    private String environment;

    public void record(LocalDate statDate, String endpoint, String method) {
        repository.increment(statDate, environment, endpoint, method);
    }

    @Transactional(readOnly = true)
    public ApiStatisticsResponse getStatistics() {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        LocalDate yesterday = today.minusDays(1);
        LocalDate last7DaysStart = today.minusDays(6);
        LocalDate last30DaysStart = today.minusDays(29);
        LocalDate sixMonthsStart = today.withDayOfMonth(1).minusMonths(5);

        List<DailyCountResponse> dailyCounts = getDailyCounts(last30DaysStart, today);
        List<MonthlyCountResponse> monthlyCounts = getMonthlyCounts(sixMonthsStart, today);
        List<EndpointCountResponse> topEndpoints = repository.findTopEndpoints(
                        last30DaysStart, today, environment)
                .stream()
                .map(item -> new EndpointCountResponse(
                        item.getEndpoint(), item.getMethod(), item.getCallCount()))
                .toList();

        return new ApiStatisticsResponse(
                environment,
                repository.findCollectionStartDate(environment),
                valueOrZero(repository.sumByDate(today, environment)),
                valueOrZero(repository.sumByDate(yesterday, environment)),
                valueOrZero(repository.sumBetween(last7DaysStart, today, environment)),
                valueOrZero(repository.sumBetween(last30DaysStart, today, environment)),
                dailyCounts,
                monthlyCounts,
                topEndpoints
        );
    }

    private List<DailyCountResponse> getDailyCounts(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> counts = repository.findDailyCounts(startDate, endDate, environment)
                .stream()
                .collect(Collectors.toMap(
                        ApiDailyStatisticRepository.DailyCount::getStatDate,
                        item -> valueOrZero(item.getCallCount())
                ));

        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> new DailyCountResponse(date, counts.getOrDefault(date, 0L)))
                .toList();
    }

    private List<MonthlyCountResponse> getMonthlyCounts(LocalDate startDate, LocalDate endDate) {
        Map<String, Long> counts = new HashMap<>();
        repository.findMonthlyCounts(startDate, endDate, environment)
                .forEach(item -> counts.put(item.getPeriod(), valueOrZero(item.getCallCount())));

        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);
        return startMonth
                .atDay(1)
                .datesUntil(endMonth.atEndOfMonth().plusDays(1))
                .filter(date -> date.getDayOfMonth() == 1)
                .map(date -> {
                    String month = YearMonth.from(date).toString();
                    return new MonthlyCountResponse(month, counts.getOrDefault(month, 0L));
                })
                .toList();
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
