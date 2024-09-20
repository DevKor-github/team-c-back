package devkor.com.teamcback.domain.bookmark.repository;

import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.common.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findAllByCategoryBookmarkList_CategoryId(Long categoryId);

    Bookmark findByLocationIdAndLocationTypeAndCategoryBookmarkList_Category(Long locationId, LocationType locationType, Category category);

    Bookmark findByLocationIdAndLocationTypeAndCategoryBookmarkList_CategoryIn(Long locationId, LocationType locationType,
        List<Category> userCategoryList);

    boolean existsByLocationIdAndLocationTypeAndCategoryBookmarkList_CategoryIn(Long locationId, LocationType locationType,
        List<Category> categories);
}
