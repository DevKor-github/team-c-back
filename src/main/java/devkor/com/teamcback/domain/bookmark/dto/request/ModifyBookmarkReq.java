package devkor.com.teamcback.domain.bookmark.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "수정된 메모")
@Getter
public class ModifyBookmarkReq {
    @Schema(description = "카테고리 메모", example = "자주 찾는 장소 모음")
    private String memo;
}
