package devkor.com.teamcback.domain.operatingtime.repositoy;

import devkor.com.teamcback.domain.operatingtime.entity.Holiday;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    boolean existsByDateAndIsHoliday(LocalDate date, boolean isHoliday);
}
