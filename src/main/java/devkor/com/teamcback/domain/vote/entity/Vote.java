package devkor.com.teamcback.domain.vote.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static devkor.com.teamcback.domain.vote.entity.VoteStatus.CLOSED;
import static devkor.com.teamcback.domain.vote.entity.VoteStatus.OPEN;
import static devkor.com.teamcback.global.response.ResultCode.CLOSED_VOTE;

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

    public void checkStatus() {
        if(this.status == CLOSED) throw new GlobalException(CLOSED_VOTE);
    }

    public void changeStatus() {
        if(this.status == CLOSED) this.status = OPEN;
        else this.status = CLOSED;
    }
}
