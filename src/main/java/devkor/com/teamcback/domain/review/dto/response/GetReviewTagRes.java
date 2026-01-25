package devkor.com.teamcback.domain.review.dto.response;

import devkor.com.teamcback.domain.review.entity.ReviewTag;
import devkor.com.teamcback.domain.review.entity.TagType;
import lombok.Getter;

@Getter
public class GetReviewTagRes {
    private Long reviewTagId;
    private String tag;
    private TagType type;

    public GetReviewTagRes(ReviewTag reviewTag) {
        this.reviewTagId = reviewTag.getId();
        this.tag = reviewTag.getTag();
        this.type = reviewTag.getType();
    }
}
