package devkor.com.teamcback.domain.review.repository;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.review.entity.PlaceReviewTagMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceReviewTagMapRepository extends JpaRepository<PlaceReviewTagMap, Long> {
    List<PlaceReviewTagMap> findByPlaceOrderByNumDesc(Place place);
}
