package devkor.com.teamcback.domain.ble.service;

import devkor.com.teamcback.domain.ble.dto.request.UpdateBLEReq;
import devkor.com.teamcback.domain.ble.entity.BLEData;
import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import devkor.com.teamcback.domain.ble.entity.BLEstatus;
import devkor.com.teamcback.domain.ble.event.PlaceBecameVacantEvent;
import devkor.com.teamcback.domain.ble.repository.BLEDataRepository;
import devkor.com.teamcback.domain.ble.repository.BLEDeviceRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BLEServiceTest {

    @Mock
    private BLEDeviceRepository bleDeviceRepository;

    @Mock
    private BLEDataRepository bleDataRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BLEService bleService;
    private BLEDevice device;

    @BeforeEach
    void setUp() {
        bleService = new BLEService(
                bleDeviceRepository,
                bleDataRepository,
                placeRepository,
                eventPublisher
        );

        Place place = new Place();
        ReflectionTestUtils.setField(place, "id", 10L);

        device = new BLEDevice();
        ReflectionTestUtils.setField(device, "id", 3L);
        device.setDeviceName("device-1");
        device.setCapacity(100);
        device.setDefaultCount(0);
        device.setRatio(1);
        device.setPlace(place);

        when(bleDeviceRepository.findByDeviceName("device-1")).thenReturn(device);
        when(bleDataRepository.save(any(BLEData.class))).thenAnswer(invocation -> {
            BLEData saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 99L);
            return saved;
        });
    }

    @Test
    void publishesEventWhenAvailableBecomesVacant() {
        when(bleDataRepository.findTopByDeviceOrderByLastTimeDescIdDesc(device))
                .thenReturn(Optional.of(data(BLEstatus.AVAILABLE)));

        bleService.updateBLE(req(20));

        ArgumentCaptor<PlaceBecameVacantEvent> captor = ArgumentCaptor.forClass(PlaceBecameVacantEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().placeId()).isEqualTo(10L);
        assertThat(captor.getValue().bleDataId()).isEqualTo(99L);
    }

    @Test
    void publishesEventWhenCrowdedBecomesVacant() {
        when(bleDataRepository.findTopByDeviceOrderByLastTimeDescIdDesc(device))
                .thenReturn(Optional.of(data(BLEstatus.CROWDED)));

        bleService.updateBLE(req(20));

        verify(eventPublisher).publishEvent(any(PlaceBecameVacantEvent.class));
    }

    @Test
    void doesNotPublishForRepeatedVacant() {
        when(bleDataRepository.findTopByDeviceOrderByLastTimeDescIdDesc(device))
                .thenReturn(Optional.of(data(BLEstatus.VACANT)));

        bleService.updateBLE(req(20));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void doesNotPublishWhenPreviousDataDoesNotExist() {
        when(bleDataRepository.findTopByDeviceOrderByLastTimeDescIdDesc(device))
                .thenReturn(Optional.empty());

        bleService.updateBLE(req(20));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void doesNotPublishWhenPreviousStatusIsFailure() {
        when(bleDataRepository.findTopByDeviceOrderByLastTimeDescIdDesc(device))
                .thenReturn(Optional.of(data(BLEstatus.FAILURE)));

        bleService.updateBLE(req(20));

        verify(eventPublisher, never()).publishEvent(any());
    }

    private UpdateBLEReq req(int lastCount) {
        UpdateBLEReq req = new UpdateBLEReq();
        req.setDeviceName("device-1");
        req.setLastCount(lastCount);
        req.setLastTime(LocalDateTime.parse("2026-08-05T10:00:00"));
        return req;
    }

    private BLEData data(BLEstatus status) {
        BLEData data = new BLEData();
        data.setDevice(device);
        data.setLastStatus(status);
        data.setLastCount(50);
        data.setLastTime(LocalDateTime.parse("2026-08-05T09:59:00"));
        return data;
    }
}
