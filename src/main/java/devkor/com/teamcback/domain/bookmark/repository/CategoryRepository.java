package devkor.com.teamcback.domain.bookmark.repository;

import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByUser(User user);
    Long countAllByUser(User user);

    @Query("SELECT c FROM CategoryBookmark cb " +
        "JOIN cb.category c " +
        "JOIN cb.bookmark b " +
        "WHERE c.user = :user AND b.locationType = :locationType AND b.locationId = :locationId")
    List<Category> findCategoriesByUserAndLocationTypeAndLocationId(
        @Param("user") User user,
        @Param("locationType") LocationType locationType,
        @Param("locationId") Long locationId
    );
}
