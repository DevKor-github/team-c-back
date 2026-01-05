package devkor.com.teamcback.domain.notification.repository;

import devkor.com.teamcback.domain.notification.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionRepository extends JpaRepository<Version, Long> {
}
