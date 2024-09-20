package devkor.com.teamcback.domain.bookmark.repository;

import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.entity.CategoryBookmark;
import devkor.com.teamcback.domain.common.LocationType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryBookmarkRepository extends JpaRepository<CategoryBookmark, Long> {
    boolean existsByCategoryAndBookmark(Category category, Bookmark bookmark);

    Optional<CategoryBookmark> findByCategoryAndBookmark(Category category, Bookmark bookmark);

    CategoryBookmark findByCategoryAndBookmarkLocationIdAndBookmarkLocationType(Category category, Long locationId, LocationType type);
}
