package devkor.com.teamcback.domain.ble.dto.response;

import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import devkor.com.teamcback.domain.ble.entity.BLEstatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UpdateBLERes {
    @Schema(description = "라운지 설치된 기기명", example = "woodang_1f_lounge")
    private String deviceName;
    @Schema(description = "최근 감지 인원", example = "10")
    private int lastCount;
    @Schema(description = "최근 Status", example = "AVAILABLE")
    private BLEstatus lastStatus;
    @Schema(description = "최근 신호 전송 시간")
    private LocalDateTime lastTime;

    public UpdateBLERes(BLEDevice device) {
        this.deviceName = device.getDeviceName();
        this.lastCount = device.getLastCount();
        this.lastStatus = device.getLastStatus();
        this.lastTime = device.getLastTime();
    }
}
