package devkor.com.teamcback.domain.transport.repository;

import devkor.com.teamcback.domain.transport.entity.Transport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportRepository extends JpaRepository<Transport, Long> {

}
