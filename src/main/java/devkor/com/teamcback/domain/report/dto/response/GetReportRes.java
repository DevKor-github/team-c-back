package devkor.com.teamcback.domain.report.dto.response;

import devkor.com.teamcback.domain.report.entity.Report;
import lombok.Getter;

@Getter
public class GetReportRes {
    private Long reportId;
    private Long targetId;
    private String targetType;
    private String reasonCategory;
    private String content;
    private String status;
    private String effectiveAt;
    private String createdAt;
    private Long reporterId;
    private Long reporterUserId;

    public GetReportRes(Report report) {
        this.reportId = report.getId();
        this.targetId = report.getTargetId();
        this.targetType = report.getTargetType().toString();
        this.reasonCategory = report.getReasonCategory().toString();
        this.content = report.getContent();
        this.status = report.getStatus().toString();
        this.effectiveAt = report.getEffectiveAt() != null ? report.getEffectiveAt().toString() : null;
        this.createdAt = report.getCreatedAt() != null ? report.getCreatedAt().toString() : null;
        this.reporterId = report.getReporter().getUserId();
        this.reporterUserId = report.getReportedUser().getUserId();
    }
}
