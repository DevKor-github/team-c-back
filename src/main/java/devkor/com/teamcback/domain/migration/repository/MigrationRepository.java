package devkor.com.teamcback.domain.migration.repository;

import devkor.com.teamcback.domain.migration.entity.Migration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationRepository extends JpaRepository<Migration, Long> {
}
