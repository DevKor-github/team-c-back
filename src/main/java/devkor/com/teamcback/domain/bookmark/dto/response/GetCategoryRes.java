package devkor.com.teamcback.domain.bookmark.dto.response;

import devkor.com.teamcback.domain.bookmark.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "카테고리 조회 완료")
@Getter
public class GetCategoryRes {
    @Schema(description = "카테고리 id", example = "1")
    private Long categoryId;
    @Schema(description = "카테고리명", example = "내 장소")
    private String category;
    @Schema(description = "카테고리 색상", example = "red")
    private String color;
    @Schema(description = "카테고리 메모", example = "자주 찾는 장소 모음")
    private String memo;
    @Schema(description = "카테고리의 즐겨찾기 수", example = "5")
    private int bookmarkCount;
    @Schema(description = "장소가 즐겨찾기된 경우 카테고리 포함 여부", example = "true")
    private boolean isBookmarked = false;

    public GetCategoryRes(Category category) {
        this.categoryId = category.getId();
        this.category = category.getCategory();
        this.color = category.getColor().getName();
        this.memo = category.getMemo();
    }

    public GetCategoryRes(Category category, int bookmarkCount) {
        this.categoryId = category.getId();
        this.category = category.getCategory();
        this.color = category.getColor().getName();
        this.memo = category.getMemo();
        this.bookmarkCount = bookmarkCount;
    }

    public GetCategoryRes(Category category, int bookmarkCount, boolean isBookmarked) {
        this.categoryId = category.getId();
        this.category = category.getCategory();
        this.color = category.getColor().getName();
        this.memo = category.getMemo();
        this.bookmarkCount = bookmarkCount;
        this.isBookmarked = isBookmarked;
    }
}
