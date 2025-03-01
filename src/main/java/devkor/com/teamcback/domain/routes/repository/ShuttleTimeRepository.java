package devkor.com.teamcback.domain.routes.repository;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.routes.entity.ShuttleTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShuttleTimeRepository extends JpaRepository<ShuttleTime, Long>{
    List<ShuttleTime> findAllByPlaceIdAndSummerSession(Place place, boolean summerSession, Sort sort);
    List<ShuttleTime> findAllBySummerSession(boolean summerSession, Sort sort);
}
