package devkor.com.teamcback.domain.report.repository;

import devkor.com.teamcback.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
