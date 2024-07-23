package devkor.com.teamcback.domain.admin.building.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "저장할 건물 별명")
@Getter
public class SaveBuildingNicknameReq {
    @Schema(description = "건물 별명", example = "우정관")
    private String nickname;
}
