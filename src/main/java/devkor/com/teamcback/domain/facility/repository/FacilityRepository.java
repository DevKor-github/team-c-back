package devkor.com.teamcback.domain.facility.repository;

import devkor.com.teamcback.domain.facility.entity.Facility;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findByNameContaining(String word);
}
