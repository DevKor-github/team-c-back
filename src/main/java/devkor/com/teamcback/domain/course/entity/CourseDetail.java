package devkor.com.teamcback.domain.course.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.common.entity.Weekday;
import devkor.com.teamcback.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Table(name = "tb_course_detail")
@NoArgsConstructor
public class CourseDetail extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Enumerated(EnumType.STRING)
    private Weekday weekday;

    @Column(nullable = false)
    private int start;

    @Column(nullable = false)
    private int end;

    @Column
    private String placeName;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @Setter
    @OneToOne
    @JoinColumn(name = "place_id")
    private Place place;

}
