package devkor.com.teamcback.domain.building.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "변경된 건물 별명 수 안내")
public class UpdateBuildingNicknamesRes {
    private String message;

    public UpdateBuildingNicknamesRes(int updatedBuildings) {
        this.message = updatedBuildings + "개의 건물이 업데이트 되었습니다.";
    }
}
