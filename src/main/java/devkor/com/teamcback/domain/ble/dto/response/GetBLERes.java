package devkor.com.teamcback.domain.ble.dto.response;

import devkor.com.teamcback.domain.ble.entity.BLEData;
import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import devkor.com.teamcback.domain.ble.entity.BLEstatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class GetBLERes {
    private Long id;
    private String deviceName;
    private Long placeId;
    private int capacity;
    private int lastCount;
    private int lastStatus;
    private LocalDateTime lastTime;

    public GetBLERes(BLEDevice device, BLEData data, BLEstatus status, int people) {
        this.id = device.getId();
        this.deviceName = device.getDeviceName();
        this.placeId = device.getPlace().getId();
        this.capacity = device.getCapacity();
        this.lastCount = people;
        this.lastStatus = status.getCode();
        this.lastTime = data.getLastTime();
    }
}
