package devkor.com.teamcback.domain.bookmark.repository;

import devkor.com.teamcback.domain.bookmark.entity.UserBookmarkLog;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBookmarkLogRepository extends JpaRepository<UserBookmarkLog, Long> {
    boolean existsByUserAndLocationIdAndLocationType(User user, Long locationId, LocationType locationType);
}
