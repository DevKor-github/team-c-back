package devkor.com.teamcback.domain.schoolcalendar.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.course.entity.Term;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Setter
    @Column
    private boolean isActive;

    @Column
    @Enumerated(EnumType.STRING)
    private Term term;

    public void updateTerm(Term term) {
        this.term = term;
    }
}
