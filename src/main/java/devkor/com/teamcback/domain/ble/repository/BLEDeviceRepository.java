package devkor.com.teamcback.domain.ble.repository;

import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import devkor.com.teamcback.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BLEDeviceRepository extends JpaRepository<BLEDevice, Long> {
    BLEDevice findByDeviceName(String deviceName);
    BLEDevice findByPlace(Place place);
    boolean existsByDeviceName(String deviceName);
    boolean existsByPlace(Place place);
}
