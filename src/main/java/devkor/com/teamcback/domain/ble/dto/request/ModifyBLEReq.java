package devkor.com.teamcback.domain.ble.dto.request;

import devkor.com.teamcback.domain.ble.entity.BLEstatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Schema(description = "BLEdevice 수정 정보")
@Getter
@Setter
public class ModifyBLEReq {
    @Schema(description = "최근 감지 인원", example = "10")
    private int lastCount;
    @Schema(description = "최근 Status", example = "AVAILABLE")
    private BLEstatus lastStatus;
    @Schema(description = "최근 신호 전송 시간")
    private LocalTime lastTime;
}
