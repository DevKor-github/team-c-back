package devkor.com.teamcback.domain.ble.entity;


import devkor.com.teamcback.domain.ble.dto.request.CreateBLEDeviceReq;
import devkor.com.teamcback.domain.ble.dto.request.ModifyBLEDeviceReq;
import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name="default_count")
    private double defaultCount;

    @Column
    private double ratio;

    public BLEDevice(CreateBLEDeviceReq req, Place place) {
        this.deviceName = req.getDeviceName();
        this.place = place;
        this.capacity = req.getCapacity();
        this.defaultCount = req.getDefaultCount();
        this.ratio = req.getRatio();
    }

    public void update(ModifyBLEDeviceReq req, Place place){
        this.deviceName = req.getDeviceName();
        this.place = place;
        this.capacity = req.getCapacity();
        this.defaultCount = req.getDefaultCount();
        this.ratio = req.getRatio();
    }

}
