package devkor.com.teamcback.domain.operatingtime.repositoy;

import devkor.com.teamcback.domain.operatingtime.entity.DayOfWeek;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingCondition;
import devkor.com.teamcback.domain.place.entity.Place;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OperatingConditionRepository extends JpaRepository<OperatingCondition, Long> {
    @Query("SELECT e FROM OperatingCondition e WHERE (e.dayOfWeek = :dayOfWeek OR e.dayOfWeek IS NULL) " +
        "AND (e.isHoliday = :isHoliday OR e.isHoliday IS NULL)" + "AND (e.isVacation = :isVacation OR e.isVacation IS NULL)")
    List<OperatingCondition> findByDayOfWeekAndIsHolidayAndIsVacationOrNot(@Param("dayOfWeek") DayOfWeek dayOfWeek, @Param("isHoliday") boolean isHoliday, @Param("isVacation") boolean isVacation);

    @Query("SELECT e FROM OperatingCondition e WHERE (e.dayOfWeek = :dayOfWeek OR e.dayOfWeek IS NULL) " +
    "AND (e.isHoliday = :isHoliday OR e.isHoliday IS NULL)" + "AND (e.isVacation = :isVacation OR e.isVacation IS NULL)" + "AND (e.place = :place)")
    OperatingCondition findByDayOfWeekAndIsHolidayAndIsVacationAndPlace(@Param("dayOfWeek") DayOfWeek dayOfWeek, @Param("isHoliday") boolean isHoliday, @Param("isVacation") boolean isVacation, @Param("place") Place place);
}
