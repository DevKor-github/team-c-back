package devkor.com.teamcback.domain.bookmark.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

@Getter
@Schema(description = "즐겨찾기 목록 조회")
public class GetBookmarkListRes {
    List<GetBookmarkRes> bookmarkList;

    public GetBookmarkListRes(List<GetBookmarkRes> bookmarkList) {
        this.bookmarkList = bookmarkList;
    }
}
