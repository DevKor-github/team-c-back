package devkor.com.teamcback.domain.SchoolCalendar.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_school_calendar")
@NoArgsConstructor
public class SchoolCalendar extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isActive;
}
