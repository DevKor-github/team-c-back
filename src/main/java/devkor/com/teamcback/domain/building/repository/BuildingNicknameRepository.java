package devkor.com.teamcback.domain.building.repository;

import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingNicknameRepository extends JpaRepository<BuildingNickname, Long> {

    List<BuildingNickname> findByNicknameContaining(String word);

    BuildingNickname findByNickname(String nickname);
}
