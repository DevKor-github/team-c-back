package devkor.com.teamcback.domain.transport.repository;

import devkor.com.teamcback.domain.transport.entity.Transport;
import devkor.com.teamcback.domain.transport.entity.TransportFloor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportFloorRepository extends JpaRepository<TransportFloor, Long> {
    TransportFloor findByTransportAndFloor(Transport transport, int floor);
}
