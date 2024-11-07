package devkor.com.teamcback.domain.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "변경된 별명 수 안내")
public class UpdateNicknamesRes {
    private String message;

    public UpdateNicknamesRes(int updatedBuildings, int updatedPlaces) {
        this.message = updatedBuildings + "개의 건물과 " + updatedPlaces + "개의 장소가 업데이트 되었습니다.";
    }
}
