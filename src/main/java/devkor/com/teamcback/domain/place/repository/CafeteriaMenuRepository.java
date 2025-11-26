package devkor.com.teamcback.domain.place.repository;

import devkor.com.teamcback.domain.place.entity.CafeteriaMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CafeteriaMenuRepository extends JpaRepository<CafeteriaMenu, Long> {
    CafeteriaMenu findByDateAndKindAndPlaceId(LocalDate date, String kind, Long placeId);

    List<CafeteriaMenu> findByDateAndPlaceId(LocalDate date, Long placeId);
}
