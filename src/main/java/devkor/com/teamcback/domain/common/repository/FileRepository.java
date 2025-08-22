package devkor.com.teamcback.domain.common.repository;

import devkor.com.teamcback.domain.common.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findAllByFileUuid(String fileUuid);

    File findByFileUuidAndSortNum(String fileUuid, Long sortNum);
}
