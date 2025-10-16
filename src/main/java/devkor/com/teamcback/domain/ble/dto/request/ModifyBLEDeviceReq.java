package devkor.com.teamcback.domain.ble.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "BLEdevice 생성 정보")
@Getter
@Setter
public class ModifyBLEDeviceReq {
    @Schema(description = "수정하고자 하는 BLEDevice ID", example = "1")
    private Long id;
    @Schema(description = "라운지 설치된 기기명", example = "woodang_1f_lounge")
    private String deviceName;
    @Schema(description ="라운지 place")
    private Long placeId;
    @Schema(description = "라운지별 최대정원", example = "20")
    private int capacity;
    @Schema(description = "위치별 기본 시간별 카운팅 횟수", example = "30")
    private int defaultCount;
    @Schema(description = "위치별 기본 시간별 평균 기기 신호 횟수", example = "100")
    private int ratio;
}
