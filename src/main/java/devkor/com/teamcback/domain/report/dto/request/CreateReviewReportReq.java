package devkor.com.teamcback.domain.report.dto.request;

import devkor.com.teamcback.domain.report.entity.ReasonCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "저장할 리뷰 신고 내용")
@Getter
@Setter
public class CreateReviewReportReq {
    @NotNull
    private ReasonCategory reasonCategory; // 신고 이유
    private String content; // 신고 내용

}
