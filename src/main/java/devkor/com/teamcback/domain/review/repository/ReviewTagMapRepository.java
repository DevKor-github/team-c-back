package devkor.com.teamcback.domain.review.repository;

import devkor.com.teamcback.domain.review.entity.ReviewTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewTagMapRepository extends JpaRepository<ReviewTag, Long> {
}
