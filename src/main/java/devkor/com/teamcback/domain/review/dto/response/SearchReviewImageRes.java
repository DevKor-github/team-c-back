package devkor.com.teamcback.domain.review.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SearchReviewImageRes {
    private Long imageId;
    private String image;

    public SearchReviewImageRes(Long imageId, String image) {
        this.imageId = imageId;
        this.image = image;
    }
}


