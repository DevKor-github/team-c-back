package devkor.com.teamcback.domain.place.repository;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceNickname;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceNicknameRepository extends JpaRepository<PlaceNickname, Long> {
    List<PlaceNickname> findByNicknameContainingOrderByNickname(String word, Pageable pageable);

    List<PlaceNickname> findAllByPlace(Place place);

    List<PlaceNickname> findByNicknameContainingAndPlaceInOrderByNickname(String word, List<Place> list, Pageable pageable);
}
