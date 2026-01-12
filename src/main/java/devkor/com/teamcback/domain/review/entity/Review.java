package devkor.com.teamcback.domain.review.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "tb_review")
@NoArgsConstructor
public class Review extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String fileUuid;    // 3장까지 업로드 가능

    @Column(nullable = false)
    private double score = 0;

    @Column(nullable = false)
    private boolean isRevisit;  // 재방문 여부

    @Column(nullable = false, length = 500)
    private String comment = "";     // 500자까지 작성 가능

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "place_id")
    private Place place;            // 식당, 카페 등의 장소

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL)
    private List<ReviewTagMap> reviewTagMaps = new ArrayList<>(); // 선택한 후기 태그
}
