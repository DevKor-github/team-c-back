package devkor.com.teamcback.domain.bookmark.repository;

import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByUser(User user);
    Long countAllByUser(User user);
}
