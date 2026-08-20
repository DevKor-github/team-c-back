package devkor.com.teamcback.domain.usagesurvey.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "tb_usage_survey_response",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_usage_survey_response_user_question",
                columnNames = {"user_id", "question_key"}
        )
)
public class UsageSurveyResponse extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_key", nullable = false, length = 40)
    private UsageSurveyQuestion questionKey;

    @Column(name = "option_key", nullable = false, length = 60)
    private String optionKey;

    public UsageSurveyResponse(
            User user,
            UsageSurveyQuestion questionKey,
            String optionKey
    ) {
        this.user = user;
        this.questionKey = questionKey;
        this.optionKey = optionKey;
    }
}
