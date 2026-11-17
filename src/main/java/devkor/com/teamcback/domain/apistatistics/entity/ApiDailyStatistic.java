package devkor.com.teamcback.domain.apistatistics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "api_daily_statistics",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_api_daily_statistics_date_env_endpoint_method",
                columnNames = {"stat_date", "environment", "endpoint", "method"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiDailyStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(nullable = false, length = 20)
    private String environment;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(name = "call_count", nullable = false)
    private Long callCount;
}
