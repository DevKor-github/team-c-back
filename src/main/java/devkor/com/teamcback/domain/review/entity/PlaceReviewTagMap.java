package devkor.com.teamcback.domain.review.entity;

import devkor.com.teamcback.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_place_review_tag_map")
@NoArgsConstructor
public class PlaceReviewTagMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "place_id")
    private Place place;

    @ManyToOne
    @JoinColumn(name = "review_tag_id")
    private ReviewTag reviewTag;

    @Column(nullable = false)
    private int num = 0;
}
