package devkor.com.teamcback.domain.common.repository;

import devkor.com.teamcback.domain.common.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
}
