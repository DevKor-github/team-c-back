package devkor.com.teamcback.domain.bookmark.repository;

import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.entity.PlaceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Bookmark findBookmarkByCategoryAndPlaceTypeAndPlaceId(Category category, PlaceType placeType, Long placeId);
    Long countAllByCategory(Category category);
    List<Bookmark> findAllByCategory(Category category);

}
