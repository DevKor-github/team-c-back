package devkor.com.teamcback.domain.routes.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalTime;

@Entity
@Getter
@Table(name = "tb_bus_stop")
public class BusStops extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "bus_stop")
    private Place busStop;
}
