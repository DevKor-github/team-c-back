package devkor.com.teamcback.domain.report.dto.response;

import devkor.com.teamcback.domain.user.entity.User;
import lombok.Getter;

import java.util.List;

@Getter
public class GetUserReviewReportStatusRes {
    private Long userId;
    private String userName;
    private boolean isReported;
    private List<GetReportedReviewRes> reviewList;
    private List<String> reportReasons;

    public GetUserReviewReportStatusRes(User user, List<GetReportedReviewRes> reviewList) {
        this.userId = user.getUserId();
        this.userName = user.getUsername();
        this.isReported = !reviewList.isEmpty();
        this.reviewList = reviewList;
        this.reportReasons = reviewList.stream().map(GetReportedReviewRes::getReasonCategory).toList();
    }
}
