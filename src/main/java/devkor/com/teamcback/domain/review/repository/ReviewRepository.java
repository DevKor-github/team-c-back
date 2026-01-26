package devkor.com.teamcback.domain.review.repository;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByPlaceOrderByCreatedAtDesc(Place place);

}
