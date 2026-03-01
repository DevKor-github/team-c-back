package devkor.com.teamcback.domain.review.dto.response;

import devkor.com.teamcback.domain.review.entity.ReviewTag;
import lombok.Getter;

@Getter
public class GetReviewTagRes {
    private Long reviewTagId;
    private String tag;

    public GetReviewTagRes(ReviewTag reviewTag) {
        this.reviewTagId = reviewTag.getId();
        this.tag = reviewTag.getTag();
    }
}
