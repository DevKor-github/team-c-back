package devkor.com.teamcback.domain.notification.repository;

import devkor.com.teamcback.domain.notification.entity.UpdateNotice;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpdateNoticeRepository extends JpaRepository<UpdateNotice, Long> {

    @EntityGraph(attributePaths = "features")
    List<UpdateNotice> findAllByPublishedAtLessThanEqualOrderByPublishedAtDescUpdateNoticeIdDesc(
            LocalDateTime publishedAt
    );
}
