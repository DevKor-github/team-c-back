package devkor.com.teamcback.domain.vote.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "tb_vote_option")
@NoArgsConstructor
public class VoteOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long voteTopicId;

    @Column(nullable = false)
    private String optionText;

    @Column(nullable = false)
    private int voteCount = 0;
}
