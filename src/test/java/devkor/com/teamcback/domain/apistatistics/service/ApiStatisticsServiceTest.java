package devkor.com.teamcback.domain.apistatistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.apistatistics.repository.ApiDailyStatisticRepository;
import devkor.com.teamcback.domain.apistatistics.repository.ApiDailyStatisticRepository.DailyCount;
import devkor.com.teamcback.domain.apistatistics.repository.ApiDailyStatisticRepository.EndpointCount;
import devkor.com.teamcback.domain.apistatistics.repository.ApiDailyStatisticRepository.MonthlyCount;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApiStatisticsServiceTest {

    @Mock
    private ApiDailyStatisticRepository repository;

    private ApiStatisticsService service;

    @BeforeEach
    void setUp() {
        service = new ApiStatisticsService(repository);
        ReflectionTestUtils.setField(service, "environment", "test");
    }

    @Test
    void returnsZeroFilledDailyAndMonthlyRanges() {
        when(repository.sumByDate(any(LocalDate.class), anyString())).thenReturn(3L);
        when(repository.sumBetween(any(LocalDate.class), any(LocalDate.class), anyString())).thenReturn(10L);
        when(repository.findDailyCounts(any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(List.of(dailyCount(LocalDate.now().minusDays(1), 2L)));
        when(repository.findMonthlyCounts(any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(List.of(monthlyCount("2026-11", 7L)));
        when(repository.findTopEndpoints(any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(List.of(endpointCount("/api/users/{userId}", "GET", 5L)));
        when(repository.findCollectionStartDate("test")).thenReturn(LocalDate.now().minusDays(10));

        var response = service.getStatistics();

        assertThat(response.getEnvironment()).isEqualTo("test");
        assertThat(response.getDailyCounts()).hasSize(30);
        assertThat(response.getMonthlyCounts()).hasSize(6);
        assertThat(response.getTopEndpoints()).singleElement().satisfies(item -> {
            assertThat(item.getEndpoint()).isEqualTo("/api/users/{userId}");
            assertThat(item.getCallCount()).isEqualTo(5L);
        });
    }

    private DailyCount dailyCount(LocalDate date, Long count) {
        return new DailyCount() {
            @Override
            public LocalDate getStatDate() {
                return date;
            }

            @Override
            public Long getCallCount() {
                return count;
            }
        };
    }

    private MonthlyCount monthlyCount(String month, Long count) {
        return new MonthlyCount() {
            @Override
            public String getPeriod() {
                return month;
            }

            @Override
            public Long getCallCount() {
                return count;
            }
        };
    }

    private EndpointCount endpointCount(String endpoint, String method, Long count) {
        return new EndpointCount() {
            @Override
            public String getEndpoint() {
                return endpoint;
            }

            @Override
            public String getMethod() {
                return method;
            }

            @Override
            public Long getCallCount() {
                return count;
            }
        };
    }
}
