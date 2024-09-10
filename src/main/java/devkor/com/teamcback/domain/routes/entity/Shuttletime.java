package devkor.com.teamcback.domain.routes.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.place.entity.Place;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import java.time.LocalTime;

@Entity
@Getter
@Table(name="tb_bus_timetable")
public class Shuttletime extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "placeId")
    private Place placeId;

    private LocalTime time;

    private boolean summerSession;
}
