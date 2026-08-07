package devkor.com.teamcback.domain.notification.repository;

import devkor.com.teamcback.domain.notification.entity.UpdateNotice;
import devkor.com.teamcback.domain.notification.entity.type.UpdateNoticeStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UpdateNoticeRepository extends
        JpaRepository<UpdateNotice, Long>,
        JpaSpecificationExecutor<UpdateNotice> {

    @EntityGraph(attributePaths = "features")
    List<UpdateNotice> findAllByStatusAndPublishedAtLessThanEqualOrderByPublishedAtDescUpdateNoticeIdDesc(
            UpdateNoticeStatus status,
            LocalDateTime publishedAt
    );
}
