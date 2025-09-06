package devkor.com.teamcback.domain.vote.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "tb_vote")
@NoArgsConstructor
public class Vote extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long voteTopicId;

    @Column
    private Long placeId;

    @Column
    @Enumerated(EnumType.STRING)
    private VoteStatus status;

    public Vote(Long voteTopicId, Long placeId, VoteStatus status) {
        this.voteTopicId = voteTopicId;
        this.placeId = placeId;
        this.status = status;
    }
}
