package devkor.com.teamcback.domain.operatingtime.entity;

import devkor.com.teamcback.domain.building.entity.Building;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_operating_time")
@NoArgsConstructor
public class OperatingTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int startHour;

    @Column(nullable = false)
    private int startMinute;

    @Column(nullable = false)
    private int endHour;

    @Column(nullable = false)
    private int endMinute;

    @ManyToOne
    @JoinColumn(name = "operating_condition_id")
    private OperatingCondition operatingCondition;
}
