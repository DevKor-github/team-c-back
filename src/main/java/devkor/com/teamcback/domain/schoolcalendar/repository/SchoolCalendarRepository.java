package devkor.com.teamcback.domain.schoolcalendar.repository;

import devkor.com.teamcback.domain.schoolcalendar.entity.SchoolCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolCalendarRepository extends JpaRepository<SchoolCalendar, Long> {
}
