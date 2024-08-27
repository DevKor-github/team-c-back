package devkor.com.teamcback.domain.operatingtime.entity;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_operating_condition")
@NoArgsConstructor
public class OperatingCondition extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek; // 평일, 토, 일

    private Boolean isEvenWeek;

    private Boolean isHoliday; // 공휴일 고려

    private Boolean isVacation; // 방학, 학기 중

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    @ManyToOne
    @JoinColumn(name = "place_id")
    private Place place;
}
