package devkor.com.teamcback.domain.common.repository;

import devkor.com.teamcback.domain.common.entity.File;
import devkor.com.teamcback.domain.review.repository.CustomFileRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<File, Long>, CustomFileRepository {
    List<File> findAllByFileUuid(String fileUuid);

    List<File> findTop10AllByFileUuidOrderBySortNumAsc(String fileUuid);

    List<File> findTop5AllByFileUuidOrderBySortNumAsc(String fileUuid);

    List<File> findTop3AllByFileUuidOrderBySortNumAsc(String fileUuid);

    File findByFileUuidAndSortNum(String fileUuid, Long sortNum);
}
