package devkor.com.teamcback.domain.course.repository;

import devkor.com.teamcback.domain.course.entity.CourseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseDetailRepository extends JpaRepository<CourseDetail, Long> {
    @Query(value = "SELECT c FROM CourseDetail c WHERE c.place IS NULL ORDER BY c.id asc LIMIT :count")
    List<CourseDetail> findLimitByPlaceIdIsNull(int count);

    List<CourseDetail> findByPlaceId(Long placeId);
}
