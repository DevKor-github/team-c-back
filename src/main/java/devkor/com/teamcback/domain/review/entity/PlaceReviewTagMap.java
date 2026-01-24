package devkor.com.teamcback.domain.review.entity;

import devkor.com.teamcback.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Setter
    @Column(nullable = false)
    private int num = 0;

    public PlaceReviewTagMap(Place place, ReviewTag tag) {
        this.place = place;
        this.reviewTag = tag;
        this.num = 1;
    }
}
