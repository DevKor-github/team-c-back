package devkor.com.teamcback.domain.character.repository;

import devkor.com.teamcback.domain.character.entity.KoCharacter;
import devkor.com.teamcback.domain.character.entity.UserCharacter;
import devkor.com.teamcback.domain.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {
    @EntityGraph(attributePaths = "character")
    List<UserCharacter> findAllByUser(User user);

    boolean existsByUserAndCharacter(User user, KoCharacter character);

    boolean existsByCharacter(KoCharacter character);

    void deleteAllByUser(User user);
}
