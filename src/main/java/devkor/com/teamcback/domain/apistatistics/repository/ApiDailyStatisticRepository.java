package devkor.com.teamcback.domain.apistatistics.repository;

import devkor.com.teamcback.domain.apistatistics.entity.ApiDailyStatistic;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ApiDailyStatisticRepository extends JpaRepository<ApiDailyStatistic, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO api_daily_statistics
                (stat_date, environment, endpoint, method, call_count)
            VALUES (:statDate, :environment, :endpoint, :method, 1)
            ON DUPLICATE KEY UPDATE call_count = call_count + 1
            """, nativeQuery = true)
    void increment(
            @Param("statDate") LocalDate statDate,
            @Param("environment") String environment,
            @Param("endpoint") String endpoint,
            @Param("method") String method
    );

    @Query("""
            SELECT COALESCE(SUM(s.callCount), 0)
            FROM ApiDailyStatistic s
            WHERE s.statDate = :statDate
              AND s.environment = :environment
            """)
    Long sumByDate(
            @Param("statDate") LocalDate statDate,
            @Param("environment") String environment
    );

    @Query("""
            SELECT COALESCE(SUM(s.callCount), 0)
            FROM ApiDailyStatistic s
            WHERE s.statDate BETWEEN :startDate AND :endDate
              AND s.environment = :environment
            """)
    Long sumBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("environment") String environment
    );

    @Query("""
            SELECT s.statDate AS statDate, SUM(s.callCount) AS callCount
            FROM ApiDailyStatistic s
            WHERE s.statDate BETWEEN :startDate AND :endDate
              AND s.environment = :environment
            GROUP BY s.statDate
            ORDER BY s.statDate
            """)
    List<DailyCount> findDailyCounts(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("environment") String environment
    );

    @Query(value = """
            SELECT DATE_FORMAT(stat_date, '%Y-%m') AS period,
                   SUM(call_count) AS callCount
            FROM api_daily_statistics
            WHERE stat_date BETWEEN :startDate AND :endDate
              AND environment = :environment
            GROUP BY DATE_FORMAT(stat_date, '%Y-%m')
            ORDER BY period
            """, nativeQuery = true)
    List<MonthlyCount> findMonthlyCounts(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("environment") String environment
    );

    @Query(value = """
            SELECT endpoint AS endpoint,
                   method AS method,
                   SUM(call_count) AS callCount
            FROM api_daily_statistics
            WHERE stat_date BETWEEN :startDate AND :endDate
              AND environment = :environment
            GROUP BY endpoint, method
            ORDER BY callCount DESC, endpoint, method
            LIMIT 10
            """, nativeQuery = true)
    List<EndpointCount> findTopEndpoints(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("environment") String environment
    );

    @Query("""
            SELECT MIN(s.statDate)
            FROM ApiDailyStatistic s
            WHERE s.environment = :environment
            """)
    LocalDate findCollectionStartDate(@Param("environment") String environment);

    @Query("""
            SELECT DISTINCT s.endpoint AS endpoint, s.method AS method
            FROM ApiDailyStatistic s
            WHERE s.environment = :environment
            ORDER BY s.endpoint, s.method
            """)
    List<EndpointMethod> findEndpointMethods(@Param("environment") String environment);

    @Query("""
            SELECT COALESCE(SUM(s.callCount), 0)
            FROM ApiDailyStatistic s
            WHERE s.statDate BETWEEN :startDate AND :endDate
              AND s.environment = :environment
              AND s.endpoint = :endpoint
              AND s.method = :method
            """)
    Long sumBetweenByEndpointAndMethod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("environment") String environment,
            @Param("endpoint") String endpoint,
            @Param("method") String method
    );

    @Query("""
            SELECT s.statDate AS statDate, SUM(s.callCount) AS callCount
            FROM ApiDailyStatistic s
            WHERE s.statDate BETWEEN :startDate AND :endDate
              AND s.environment = :environment
              AND s.endpoint = :endpoint
              AND s.method = :method
            GROUP BY s.statDate
            ORDER BY s.statDate
            """)
    List<DailyCount> findDailyCountsByEndpointAndMethod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("environment") String environment,
            @Param("endpoint") String endpoint,
            @Param("method") String method
    );

    @Query(value = """
            SELECT DATE_FORMAT(stat_date, '%Y-%m') AS period,
                   SUM(call_count) AS callCount
            FROM api_daily_statistics
            WHERE stat_date BETWEEN :startDate AND :endDate
              AND environment = :environment
              AND endpoint = :endpoint
              AND method = :method
            GROUP BY DATE_FORMAT(stat_date, '%Y-%m')
            ORDER BY period
            """, nativeQuery = true)
    List<MonthlyCount> findMonthlyCountsByEndpointAndMethod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("environment") String environment,
            @Param("endpoint") String endpoint,
            @Param("method") String method
    );

    @Query("""
            SELECT MIN(s.statDate)
            FROM ApiDailyStatistic s
            WHERE s.environment = :environment
              AND s.endpoint = :endpoint
              AND s.method = :method
            """)
    LocalDate findCollectionStartDateByEndpointAndMethod(
            @Param("environment") String environment,
            @Param("endpoint") String endpoint,
            @Param("method") String method
    );

    interface EndpointMethod {
        String getEndpoint();

        String getMethod();
    }

    interface DailyCount {
        LocalDate getStatDate();

        Long getCallCount();
    }

    interface MonthlyCount {
        String getPeriod();

        Long getCallCount();
    }

    interface EndpointCount {
        String getEndpoint();

        String getMethod();

        Long getCallCount();
    }
}
