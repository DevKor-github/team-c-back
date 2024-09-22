package devkor.com.teamcback.domain.koyeon.entity;

import devkor.com.teamcback.domain.routes.entity.Node;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_free_pub")
@NoArgsConstructor
public class FreePub {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sponsor;

//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "nodeId")
//    private Node node;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String operatingTime;

    @Column(nullable = false)
    private String menus;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;
}
