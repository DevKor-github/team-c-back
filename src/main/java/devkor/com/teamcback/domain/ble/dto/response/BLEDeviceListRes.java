package devkor.com.teamcback.domain.ble.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BLEDeviceListRes {

    private Long id;
    private String deviceName;
    private Long placeId;
    private Integer capacity;

    public BLEDeviceListRes(Long id, String deviceName, Long placeId, int capacity) {
        this.id = id;
        this.deviceName = deviceName;
        this.placeId = placeId;
        this.capacity = capacity;
    }
}