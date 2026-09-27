package devkor.com.teamcback.domain.place.repository;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceNickname;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import devkor.com.teamcback.domain.chatbot.search.ChatbotPlaceCandidate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceNicknameRepository extends JpaRepository<PlaceNickname, Long> {
    @EntityGraph(attributePaths = {"place", "place.building"})
    List<PlaceNickname> findAllByChosungContainingOrderByNickname(String chosung, Pageable pageable);
    @EntityGraph(attributePaths = {"place", "place.building"})
    List<PlaceNickname> findAllByJasoDecomposeContainingOrderByNickname(String jaso, Pageable pageable);

    List<PlaceNickname> findAllByPlace(Place place);

    @EntityGraph(attributePaths = {"place", "place.building"})
    List<PlaceNickname> findByChosungContainingAndPlaceInOrderByNickname(String chosung, List<Place> list, Pageable pageable);
    @EntityGraph(attributePaths = {"place", "place.building"})
    List<PlaceNickname> findByJasoDecomposeContainingAndPlaceInOrderByNickname(String jaso, List<Place> list, Pageable pageable);
    List<PlaceNickname> findByChosungIsNullOrJasoDecomposeIsNull();
    List<PlaceNickname> findAllByNicknameContaining(String blank);

    @Query("select new devkor.com.teamcback.domain.chatbot.search.ChatbotPlaceCandidate(p.id, p.name, b.id, b.name, p.floor, p.type, p.detail, n.nickname) "
            + "from PlaceNickname n join n.place p join p.building b where n.jasoDecompose like concat('%', :jaso, '%')")
    List<ChatbotPlaceCandidate> findChatbotByJaso(@Param("jaso") String jaso, Pageable pageable);

    @Query("select new devkor.com.teamcback.domain.chatbot.search.ChatbotPlaceCandidate(p.id, p.name, b.id, b.name, p.floor, p.type, p.detail, n.nickname) "
            + "from PlaceNickname n join n.place p join p.building b where n.chosung like concat('%', :chosung, '%')")
    List<ChatbotPlaceCandidate> findChatbotByChosung(@Param("chosung") String chosung, Pageable pageable);

    @Query("select new devkor.com.teamcback.domain.chatbot.search.ChatbotPlaceCandidate(p.id, p.name, b.id, b.name, p.floor, p.type, p.detail, n.nickname) "
            + "from PlaceNickname n join n.place p join p.building b where b.id in :buildingIds "
            + "and n.jasoDecompose like concat('%', :jaso, '%')")
    List<ChatbotPlaceCandidate> findChatbotByJasoAndBuildingIds(@Param("jaso") String jaso,
                                                                  @Param("buildingIds") List<Long> buildingIds,
                                                                  Pageable pageable);

    @Query("select new devkor.com.teamcback.domain.chatbot.search.ChatbotPlaceCandidate(p.id, p.name, b.id, b.name, p.floor, p.type, p.detail, n.nickname) "
            + "from PlaceNickname n join n.place p join p.building b where b.id in :buildingIds "
            + "and n.chosung like concat('%', :chosung, '%')")
    List<ChatbotPlaceCandidate> findChatbotByChosungAndBuildingIds(@Param("chosung") String chosung,
                                                                    @Param("buildingIds") List<Long> buildingIds,
                                                                    Pageable pageable);
}
