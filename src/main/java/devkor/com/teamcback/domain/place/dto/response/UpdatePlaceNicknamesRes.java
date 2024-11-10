package devkor.com.teamcback.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "변경된 장소 별명 수 안내")
public class UpdatePlaceNicknamesRes {
    private String message;

    public UpdatePlaceNicknamesRes(int updatedPlaces) {
        this.message = updatedPlaces + "개의 장소가 업데이트 되었습니다.";
    }
}
