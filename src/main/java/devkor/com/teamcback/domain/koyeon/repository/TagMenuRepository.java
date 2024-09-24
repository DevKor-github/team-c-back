package devkor.com.teamcback.domain.koyeon.repository;

import devkor.com.teamcback.domain.koyeon.entity.FoodTag;
import devkor.com.teamcback.domain.koyeon.entity.Menu;
import devkor.com.teamcback.domain.koyeon.entity.TagMenu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagMenuRepository extends JpaRepository<TagMenu, Long> {

    List<TagMenu> findByFoodTag(FoodTag tag);
}
