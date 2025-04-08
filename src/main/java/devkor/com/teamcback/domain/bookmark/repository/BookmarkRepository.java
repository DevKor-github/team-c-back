package devkor.com.teamcback.domain.bookmark.repository;

import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.common.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    @Query("select cb.bookmark from CategoryBookmark cb where cb.category.id = :categoryId")
    List<Bookmark> findAllByCategoryBookmarkList_CategoryId(@Param("categoryId") Long categoryId);

    @Query("select b from Bookmark b join b.categoryBookmarkList cb where b.locationId = :locationId and b.locationType = :locationType and cb.category = :category")
    Bookmark findByLocationIdAndLocationTypeAndCategoryBookmarkList_Category(@Param("locationId")Long locationId, @Param("locationType")LocationType locationType, @Param("category")Category category);

    @Query("select b from Bookmark b join b.categoryBookmarkList cb where b.locationId = :locationId and b.locationType = :locationType and cb.category IN :categories")
    Bookmark findByLocationIdAndLocationTypeAndCategoryBookmarkList_CategoryIn(@Param("locationId")Long locationId, @Param("locationType")LocationType locationType,
                                                                               @Param("categories")List<Category> userCategoryList);

    @Query("select case when count(b) > 0 then true else false end from Bookmark b join b.categoryBookmarkList cb where b.locationId = :locationId and b.locationType = :locationType and cb.category IN :categories")
    boolean existsByLocationIdAndLocationTypeAndCategoryBookmarkList_CategoryIn(@Param("locationId")Long locationId, @Param("locationType")LocationType locationType,
                                                                                @Param("categories") List<Category> categories);
}
