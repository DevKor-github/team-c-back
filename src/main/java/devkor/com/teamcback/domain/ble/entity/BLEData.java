package devkor.com.teamcback.domain.ble.entity;


import devkor.com.teamcback.domain.ble.dto.request.UpdateBLEReq;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="tb_ble_data")
@Getter
@Setter
@NoArgsConstructor
public class BLEData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private BLEDevice device;

    @Column
    private Integer lastCount;

    @Enumerated(EnumType.ORDINAL)
    @Column
    private BLEstatus lastStatus;

    @Column(nullable = false)
    private LocalDateTime lastTime;

}
