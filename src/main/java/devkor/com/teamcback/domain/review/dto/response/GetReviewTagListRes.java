package devkor.com.teamcback.domain.review.dto.response;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class GetReviewTagListRes {
    private Map<String, List<GetReviewTagRes>> reviewTags;

    public GetReviewTagListRes(Map<String, List<GetReviewTagRes>> reviewTags) {
        this.reviewTags = reviewTags;
    }
}
