package devkor.com.teamcback.domain.report.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "tb_report")
@NoArgsConstructor
public class Report extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @Column
    private Long targetId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReasonCategory reasonCategory;

    @Column(length = 300)
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Column
    private LocalDate effectiveAt; // 신고 정지 시작일

    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;

    public Report(TargetType targetType, Long targetId, ReasonCategory reasonCategory, String content, ReportStatus status, User reporter, User reportedUser) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonCategory = reasonCategory;
        this.content = content;
        this.status = status;
        this.reporter = reporter;
        this.reportedUser = reportedUser;
    }
}
