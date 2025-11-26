package devkor.com.teamcback.domain.ble.dto.response;

import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "BLEdevice 생성 정보")
@Getter
public class CreateBLEDeviceRes {
    private Long id;

    public CreateBLEDeviceRes(BLEDevice bleDevice) {
        this.id = bleDevice.getId();
    }
}
