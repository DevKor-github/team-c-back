package devkor.com.teamcback.domain.ble.entity;


import devkor.com.teamcback.domain.ble.dto.request.CreateBLEReq;
import devkor.com.teamcback.domain.ble.dto.request.ModifyBLEReq;
import devkor.com.teamcback.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name="tb_ble_device")
@Getter
@Setter
@NoArgsConstructor
public class BLEdevice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String deviceName;

    @Column
    private Long placeId;

    @Column
    private int capacity;

    @Column
    private int lastCount;

    @Column
    private BLEstatus lastStatus;

    @Column
    private LocalTime lastTime;

    public BLEdevice(CreateBLEReq req){
        this.deviceName = req.getDeviceName();
        this.placeId = req.getPlaceId();
        this.capacity = req.getCapacity();
    }

    public void update(ModifyBLEReq req){
        this.lastCount = req.getLastCount();
        this.lastStatus = req.getLastStatus();
        this.lastTime = req.getLastTime();
    }

}
