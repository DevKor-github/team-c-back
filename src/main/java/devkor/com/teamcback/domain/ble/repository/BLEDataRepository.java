package devkor.com.teamcback.domain.ble.repository;

import devkor.com.teamcback.domain.ble.entity.BLEData;
import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface BLEDataRepository extends JpaRepository<BLEData, Long> {
    Optional<BLEData> findTopByDeviceOrderByLastTimeDesc(BLEDevice device);
}
