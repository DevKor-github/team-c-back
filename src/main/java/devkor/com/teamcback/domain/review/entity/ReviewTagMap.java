package devkor.com.teamcback.domain.review.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_review_tag_map")
@NoArgsConstructor
public class ReviewTagMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 하나의 리뷰에 태그 최대 5개
    @ManyToOne
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne
    @JoinColumn(name = "review_tag_id")
    private ReviewTag reviewTag;
}
