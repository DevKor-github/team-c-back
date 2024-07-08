package devkor.com.teamcback.domain.bookmark.dto.request;

import devkor.com.teamcback.domain.bookmark.entity.Color;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "카테고리 이름, 색상, 메모")
@Getter
public class ModifyCategoryReq {
    @Schema(description = "카테고리명", example = "내 장소")
    private String category;
    @Schema(description = "카테고리 색상", example = "red")
    private Color color;
    @Schema(description = "카테고리 메모", example = "자주 찾는 장소 모음")
    private String memo;
}
