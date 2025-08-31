package devkor.com.teamcback.domain.ble.dto.request;

import devkor.com.teamcback.domain.ble.entity.BLEstatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Schema(description = "BLEdevice 생성 정보")
@Getter
@Setter
public class CreateBLEReq {
    @Schema(description = "라운지 설치된 기기명", example = "woodang_1f_lounge")
    private String deviceName;
    @Schema(description ="라운지 placeId" , example = "2499")
    private Long placeId;
    @Schema(description = "라운지별 최대정원", example = "20")
    private int capacity;
}
