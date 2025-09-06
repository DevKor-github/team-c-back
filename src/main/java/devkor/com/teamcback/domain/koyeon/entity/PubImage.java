package devkor.com.teamcback.domain.koyeon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_pub_image")
@NoArgsConstructor
public class PubImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl; // 추후 삭제

    @Column(nullable = false)
    private String fileUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freePub_id")
    private FreePub freePub;

}
