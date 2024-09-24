package devkor.com.teamcback.domain.koyeon.repository;

import devkor.com.teamcback.domain.koyeon.entity.FreePub;
import devkor.com.teamcback.domain.koyeon.entity.Menu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByFreePub(FreePub pub);
}
