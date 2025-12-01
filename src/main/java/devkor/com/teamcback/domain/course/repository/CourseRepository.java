package devkor.com.teamcback.domain.course.repository;

import devkor.com.teamcback.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
