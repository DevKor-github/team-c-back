package devkor.com.teamcback.domain.review.repository;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.review.entity.Review;
import devkor.com.teamcback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByPlaceOrderByCreatedAtDesc(Place place);

    /**
     * 특정 사용자가 특정 장소에 특정 기간 내 작성한 리뷰 존재 여부 확인
     * (같은 장소 하루 1개 리뷰 제한용)
     */
    boolean existsByUserAndPlaceAndCreatedAtBetween(User user, Place place, LocalDateTime start, LocalDateTime end);
}
