package devkor.com.teamcback.domain.report.dto.request;

import devkor.com.teamcback.domain.report.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "수정할 신고 내용")
@Getter
@Setter
public class UpdateReportStatusReq {
    @NotNull
    private ReportStatus status;
}
