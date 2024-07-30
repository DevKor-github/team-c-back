package devkor.com.teamcback.domain.operatingtime.repositoy;

import devkor.com.teamcback.domain.operatingtime.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

}
