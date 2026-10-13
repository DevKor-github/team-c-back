package devkor.com.teamcback.domain.building.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import devkor.com.teamcback.domain.chatbot.search.ChatbotBuildingCandidate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("select new devkor.com.teamcback.domain.chatbot.search.ChatbotBuildingCandidate(b.id, b.name, n.nickname) "
            + "from BuildingNickname n join n.building b where n.jasoDecompose like concat('%', :jaso, '%')")
    List<ChatbotBuildingCandidate> findChatbotByJaso(@Param("jaso") String jaso, Pageable pageable);

    @Query("select new devkor.com.teamcback.domain.chatbot.search.ChatbotBuildingCandidate(b.id, b.name, n.nickname) "
            + "from BuildingNickname n join n.building b where n.chosung like concat('%', :chosung, '%')")
    List<ChatbotBuildingCandidate> findChatbotByChosung(@Param("chosung") String chosung, Pageable pageable);
}
