package devkor.com.teamcback.domain.report.repository;

import devkor.com.teamcback.domain.report.entity.Report;
import devkor.com.teamcback.domain.report.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long>, CustomReportRepository {

    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<Report> findAllByOrderByCreatedAtDesc();

    List<Report> findAllByStatus(ReportStatus reportStatus);
}
