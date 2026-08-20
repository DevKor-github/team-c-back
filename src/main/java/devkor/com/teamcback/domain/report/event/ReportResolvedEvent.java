package devkor.com.teamcback.domain.report.event;

import devkor.com.teamcback.domain.report.entity.ReportStatus;

public record ReportResolvedEvent(
        Long reportId,
        Long reporterUserId,
        ReportStatus finalStatus
) {
}
