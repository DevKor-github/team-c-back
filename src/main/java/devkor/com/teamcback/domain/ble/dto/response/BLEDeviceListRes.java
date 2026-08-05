package devkor.com.teamcback.domain.ble.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BLEDeviceListRes {

    private Long id;
    private String deviceName;
    private Long placeId;
    private Integer capacity;
    private String imageUrl;
    private Integer lastCount;
    private Integer lastStatus;
    private LocalDateTime lastTime;

    public BLEDeviceListRes(Long id, String deviceName, Long placeId, int capacity, String imageUrl,
                            Integer lastCount, Integer lastStatus, LocalDateTime lastTime) {
        this.id = id;
        this.deviceName = deviceName;
        this.placeId = placeId;
        this.capacity = capacity;
        this.imageUrl = imageUrl;
        this.lastCount = lastCount;
        this.lastStatus = lastStatus;
        this.lastTime = lastTime;
    }
}