package devkor.com.teamcback.domain.review.repository;

import devkor.com.teamcback.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

}
