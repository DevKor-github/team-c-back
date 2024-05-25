package devkor.com.teamcback.domain.transport.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.transport.entity.Transport;
import devkor.com.teamcback.domain.transport.entity.TransportType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportRepository extends JpaRepository<Transport, Long> {
    List<Transport> findAllByBuilding(Building building);
}
