package devkor.com.teamcback.domain.place.repository;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceNickname;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceNicknameRepository extends JpaRepository<PlaceNickname, Long> {
    List<PlaceNickname> findAllByChosungContainingOrderByNickname(String chosung, Pageable pageable);
    List<PlaceNickname> findAllByJasoDecomposeContainingOrderByNickname(String jaso, Pageable pageable);

    List<PlaceNickname> findAllByPlace(Place place);

    List<PlaceNickname> findByChosungContainingAndPlaceInOrderByNickname(String chosung, List<Place> list, Pageable pageable);
    List<PlaceNickname> findByJasoDecomposeContainingAndPlaceInOrderByNickname(String jaso, List<Place> list, Pageable pageable);
    List<PlaceNickname> findByChosungIsNullOrJasoDecomposeIsNull();
    List<PlaceNickname> findAllByNicknameContaining(String blank);
}
