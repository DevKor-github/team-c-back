package devkor.com.teamcback.domain.routes.repository;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.routes.entity.ShuttleTime;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShuttleTimeRepository extends JpaRepository<ShuttleTime, Long> {
    List<ShuttleTime> findAllByPlaceAndSummerSession(Place place, boolean isSummerSession, Sort sort);

    List<ShuttleTime> findAllBySummerSession(boolean isSummerSession, Sort sort);
}
