package devkor.com.teamcback.domain.operatingtime.repositoy;

import devkor.com.teamcback.domain.operatingtime.entity.DayOfWeek;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingCondition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatingConditionRepository extends JpaRepository<OperatingCondition, Long> {
    List<OperatingCondition> findAllByDayOfWeekAndIsVacation(DayOfWeek dayOfWeek, boolean isVacation);
}
