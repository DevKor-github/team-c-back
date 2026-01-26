package devkor.com.teamcback.domain.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "장소의 리뷰 태그 목록")
@Getter
@NoArgsConstructor
public class SearchPlaceReviewTagRes {
    @Schema(description = "리뷰 태그 id", example = "1")
    private Long id;

    @Schema(description = "태그 내용", example = "맛있어요")
    private String tag;

    @Schema(description = "태그 개수", example = "5")
    private int num;

    public SearchPlaceReviewTagRes(Long id, String tag, int num) {
        this.id = id;
        this.tag = tag;
        this.num = num;
    }
}
