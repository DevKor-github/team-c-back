package devkor.com.teamcback.domain.routes.repository;

import devkor.com.teamcback.domain.routes.entity.BusStops;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BusStopRepository extends JpaRepository<BusStops, Long> {
}
