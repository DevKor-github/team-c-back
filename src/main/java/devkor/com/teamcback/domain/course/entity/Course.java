package devkor.com.teamcback.domain.course.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_course")
@NoArgsConstructor
public class Course extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Term term;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private int unit;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String section;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String professor;
}
