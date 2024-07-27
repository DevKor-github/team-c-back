package devkor.com.teamcback.domain.bookmark.dto.response;

import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "즐겨찾기 생성 완료")
@Getter
public class CreateBookmarkRes {
    @Schema(description = "즐겨찾기 id", example = "1")
    private Long bookmarkId;

    public CreateBookmarkRes(Bookmark bookmark) {
        this.bookmarkId = bookmark.getId();
    }
}
