package devkor.com.teamcback.domain.vote.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "tb_vote_record")
@NoArgsConstructor
public class VoteRecord extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long placeId;

    @Column(nullable = false)
    private Long voteTopicId;

    @Column(nullable = false)
    private Long voteOptionId;

    public VoteRecord(Long userId, Long placeId, Long voteTopicId, Long voteOptionId) {
        this.userId = userId;
        this.placeId = placeId;
        this.voteTopicId = voteTopicId;
        this.voteOptionId = voteOptionId;
    }
}
