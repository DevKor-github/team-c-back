package devkor.com.teamcback.domain.bookmark.dto.request;

import devkor.com.teamcback.domain.common.LocationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "장소타입, 장소 id (건물 or 강의실 or 편의시설), 메모")
@Getter
@AllArgsConstructor
public class CreateBookmarkReq {
    @Schema(description = "카테고리 id 리스트", example = "[1, 2, 3]")
    private List<Long> categoryIdList;

    @Schema(description = "장소타입", example = "BUILDING")
    private LocationType locationType;

    @Schema(description = "장소 id", example = "5")
    private Long locationId;

    @Schema(description = "즐겨찾기 메모", example = "수업 장소")
    private String memo;
}
