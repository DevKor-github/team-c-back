package devkor.com.teamcback.domain.ble.repository;

import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.chatbot.crowd.CrowdPlaceCandidate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BLEDeviceRepository extends JpaRepository<BLEDevice, Long> {
    BLEDevice findByDeviceName(String deviceName);
    BLEDevice findByPlace(Place place);
    boolean existsByDeviceName(String deviceName);
    boolean existsByPlace(Place place);

    @Query("select new devkor.com.teamcback.domain.chatbot.crowd.CrowdPlaceCandidate(" +
            "d.place.id, d.deviceName, d.place.name, d.place.floor, d.place.type, " +
            "d.place.building.id, d.place.building.name) " +
            "from BLEDevice d where d.place.building.id = :buildingId")
    List<CrowdPlaceCandidate> findChatbotCrowdPlacesByBuildingId(@Param("buildingId") Long buildingId);

    @Query("select new devkor.com.teamcback.domain.chatbot.crowd.CrowdPlaceCandidate(" +
            "d.place.id, d.deviceName, d.place.name, d.place.floor, d.place.type, " +
            "d.place.building.id, d.place.building.name) " +
            "from BLEDevice d")
    List<CrowdPlaceCandidate> findAllChatbotCrowdPlaces();
}
