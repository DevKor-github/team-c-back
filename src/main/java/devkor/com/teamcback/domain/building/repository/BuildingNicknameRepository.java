package devkor.com.teamcback.domain.building.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface BuildingNicknameRepository extends JpaRepository<BuildingNickname, Long> {

    @EntityGraph(attributePaths = "building")
    List<BuildingNickname> findAllByChosungContaining(String chosung);
    @EntityGraph(attributePaths = "building")
    List<BuildingNickname> findAllByJasoDecomposeContaining(String jaso);
    @EntityGraph(attributePaths = "building")
    List<BuildingNickname> findAllByChosungContainingOrderByNickname(String chosung, Pageable pageable);
    @EntityGraph(attributePaths = "building")
    List<BuildingNickname> findAllByJasoDecomposeContainingOrderByNickname(String jaso, Pageable pageable);
    List<BuildingNickname> findByChosungIsNullOrJasoDecomposeIsNull();
    List<BuildingNickname> findAllByBuilding(Building building);
    List<BuildingNickname> findAllByNicknameContaining(String blank);
}
