package devkor.com.teamcback.domain.koyeon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_free_pub_nickname")
@NoArgsConstructor
public class FreePubNickname {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    @ManyToOne
    @JoinColumn(name = "free_pub_id", nullable = false)
    private FreePub freePub;
}
