package devkor.com.teamcback.domain.building.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildingNicknameRepository extends JpaRepository<BuildingNickname, Long> {

    List<BuildingNickname> findAllByChosungContaining(String chosung);
    List<BuildingNickname> findAllByJasoDecomposeContaining(String jaso);
    List<BuildingNickname> findByChosungIsNullOrJasoDecomposeIsNull();
    List<BuildingNickname> findAllByBuilding(Building building);
}
