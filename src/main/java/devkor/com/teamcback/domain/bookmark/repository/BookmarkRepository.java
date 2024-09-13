package devkor.com.teamcback.domain.bookmark.repository;

import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.common.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByCategoryAndLocationTypeAndLocationId(Category category, LocationType locationType, Long locationId);
    Long countAllByCategory(Category category);
    List<Bookmark> findAllByCategory(Category category);
    Boolean existsByLocationTypeAndLocationIdAndCategoryIn(LocationType locationType, Long locationId, List<Category> category);
}
