package devkor.com.teamcback.domain.report.repository;

import devkor.com.teamcback.domain.report.entity.Report;
import devkor.com.teamcback.domain.report.entity.ReportStatus;
import devkor.com.teamcback.domain.report.entity.TargetType;
import devkor.com.teamcback.domain.user.entity.User;

import java.util.List;

public interface CustomReportRepository {
    List<Report> findUniqueReportsForUserReviewReportStatus(User user, TargetType type, ReportStatus status);
}
