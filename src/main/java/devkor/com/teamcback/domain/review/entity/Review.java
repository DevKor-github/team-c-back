package devkor.com.teamcback.domain.review.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.review.dto.request.CreateReviewReq;
import devkor.com.teamcback.domain.review.dto.request.ModifyReviewReq;
import devkor.com.teamcback.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(nullable = false)
    private boolean isReported = false;  // 신고 여부

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "place_id")
    private Place place;            // 식당, 카페 등의 장소

    @Setter
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL)
    private List<ReviewTagMap> reviewTagMaps = new ArrayList<>(); // 선택한 후기 태그

    public Review(CreateReviewReq createReviewReq, User user, Place place) {
        this.fileUuid = UUID.randomUUID().toString();
        this.score = createReviewReq.getScore();
        this.isRevisit = createReviewReq.isRevisit();
        this.comment = createReviewReq.getComment();
        this.user = user;
        this.place = place;
    }

    public void modify(@Valid ModifyReviewReq modifyReviewReq) {
        this.score = modifyReviewReq.getScore();
        this.isRevisit = modifyReviewReq.isRevisit();
        this.comment = modifyReviewReq.getComment();
    }
}
