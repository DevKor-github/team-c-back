package devkor.com.teamcback.domain.place.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "저장할 강의실 별명")
@Getter
public class SavePlaceNicknameReq {
    @Schema(description = "강의실 별명", example = "강의준비실")
    private String nickname;
}
