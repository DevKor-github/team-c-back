package devkor.com.teamcback.domain.SchoolCalendar.repository;

import devkor.com.teamcback.domain.SchoolCalendar.entity.SchoolCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolCalendarRepository extends JpaRepository<SchoolCalendar, Long> {
}
