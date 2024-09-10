package devkor.com.teamcback.domain.routes.repository;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.routes.entity.Shuttletime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShuttletimeRepository extends JpaRepository<Shuttletime, Long> {
    List<Shuttletime> findAllByPlaceIdAndSummerSession(Place place, boolean summerSession);

}
