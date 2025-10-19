package devkor.com.teamcback.domain.place.repository;

import devkor.com.teamcback.domain.place.entity.CafeteriaMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface CafeteriaMenuRepository extends JpaRepository<CafeteriaMenu, Long> {
    boolean existsByDateAndKindAndPlaceId(LocalDate date, String kind, Long placeId);
}
