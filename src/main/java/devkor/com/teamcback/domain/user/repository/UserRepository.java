package devkor.com.teamcback.domain.user.repository;

import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsernameAndUserIdNot(String username, Long id);

    boolean existsByUsername(String username);

    User findByEmailAndProvider(String email, Provider provider);

    User findByUserId(long userId);

    List<User> findAllByEmail(String email);
}
