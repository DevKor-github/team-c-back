package devkor.com.teamcback.domain.character.repository;

import devkor.com.teamcback.domain.character.entity.KoCharacter;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<KoCharacter, Long> {
    List<KoCharacter> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    List<KoCharacter> findAllByOrderByDisplayOrderAsc();

    boolean existsByName(String name);

    Optional<KoCharacter> findByName(String name);
}
