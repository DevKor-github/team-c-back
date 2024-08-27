package devkor.com.teamcback.domain.bookmark.dto.response;

import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import devkor.com.teamcback.domain.common.LocationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "카테고리의 전체 즐겨찾기 조회 완료")
@Getter
public class GetBookmarkRes {
    @Schema(description = "즐겨찾기 id", example = "1")
    private Long bookmarkId;

    @Schema(description = "즐겨찾기 장소 타입", example = "CLASSROOM")
    private LocationType locationType;

    @Schema(description = "즐겨찾기 장소 Id", example = "5")
    private Long placeId;

    @Schema(description = "즐겨찾기 메모", example = "자료구조 강의실")
    private String memo;

    public GetBookmarkRes(Bookmark bookmark) {
        this.bookmarkId = bookmark.getId();
        this.locationType = bookmark.getLocationType();
        this.placeId = bookmark.getPlaceId();
        this.memo = bookmark.getMemo();
    }

}
