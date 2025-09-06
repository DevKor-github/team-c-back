package devkor.com.teamcback.domain.ble.entity;


import devkor.com.teamcback.domain.ble.dto.request.UpdateBLEReq;
import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="tb_ble_device")
@Getter
@Setter
@NoArgsConstructor
public class BLEDevice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String deviceName;

    @OneToOne
    @JoinColumn(name = "placeId")
    private Place place;

    @Column
    private int capacity;

    @Column
    private int lastCount;

    @Column
    private BLEstatus lastStatus;

    @Column
    private LocalDateTime lastTime;

    public BLEDevice(String deviceName, Place place, int capacity) {
        this.deviceName = deviceName;
        this.place = place;
        this.capacity = capacity;
    }

    public void update(UpdateBLEReq req, BLEstatus status) {
        this.lastCount = req.getLastCount();
        this.lastStatus = status;
        this.lastTime = req.getLastTime();
    }

    public void update(BLEstatus status){
        this.lastStatus = status;
    }

}
