package devkor.com.teamcback.domain.ble.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import devkor.com.teamcback.domain.ble.entity.BLEstatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "BLEdevice 수정 정보")
@Getter
@Setter
public class UpdateBLEReq {
    @Schema(description = "라운지 설치된 기기명", example = "SKFutureHall_5f_lounge_517")
    private String deviceName;
    @Schema(description = "최근 감지 신호 개수", example = "10")
    private int lastCount;
    @Schema(description = "최근 신호 전송 시간")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastTime;
}
