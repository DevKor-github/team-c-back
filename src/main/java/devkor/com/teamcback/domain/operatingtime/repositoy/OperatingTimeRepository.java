package devkor.com.teamcback.domain.operatingtime.repositoy;

import devkor.com.teamcback.domain.operatingtime.entity.OperatingCondition;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatingTimeRepository extends JpaRepository<OperatingTime, Long> {

    List<OperatingTime> findAllByOperatingCondition(OperatingCondition operatingCondition);
}
