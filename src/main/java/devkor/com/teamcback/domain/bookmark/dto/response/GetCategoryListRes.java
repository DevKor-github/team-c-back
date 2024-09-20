package devkor.com.teamcback.domain.bookmark.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Schema(description = "카테고리 목록 조회")
@NoArgsConstructor
public class GetCategoryListRes {
    @Schema(description = "사용자의 카테고리 목록")
    private List<GetCategoryRes> categoryList;
    @Schema(description = "장소가 즐겨찾기된 경우 즐겨찾기 id", example = "1")
    @Setter
    private Long bookmarkId = null;

    public GetCategoryListRes(List<GetCategoryRes> categoryList) {
        this.categoryList = categoryList;
    }
}
