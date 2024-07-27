package devkor.com.teamcback.domain.bookmark.dto.request;

import devkor.com.teamcback.domain.bookmark.entity.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "장소타입, 장소 id (건물 or 강의실 or 편의시설), 메모")
@Getter
public class CreateBookmarkReq {

    @Schema(description = "장소타입", example = "BUILDING")
    private PlaceType placeType;

    @Schema(description = "장소 id", example = "5")
    private Long placeId;

    @Schema(description = "카테고리 메모", example = "자주 찾는 장소 모음")
    private String memo;
}
