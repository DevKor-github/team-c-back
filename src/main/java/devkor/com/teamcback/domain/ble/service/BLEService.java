package devkor.com.teamcback.domain.ble.service;


import devkor.com.teamcback.domain.ble.dto.request.UpdateBLEReq;
import devkor.com.teamcback.domain.ble.dto.response.GetBLERes;
import devkor.com.teamcback.domain.ble.dto.response.UpdateBLERes;
import devkor.com.teamcback.domain.ble.entity.BLEData;
import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import devkor.com.teamcback.domain.ble.entity.BLEstatus;
import devkor.com.teamcback.domain.ble.repository.BLEDataRepository;
import devkor.com.teamcback.domain.ble.repository.BLEDeviceRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_PLACE;

@Slf4j
@Service
@RequiredArgsConstructor
public class BLEService {
    private final BLEDeviceRepository bledeviceRepository;
    private final BLEDataRepository bleDataRepository;
    private final PlaceRepository placeRepository;

    @Transactional
    public UpdateBLERes updateBLEDevice(UpdateBLEReq updateBLEReq) {
        List<BLEDevice> deviceList = bledeviceRepository.findByDeviceName(updateBLEReq.getDeviceName());
        if (deviceList.isEmpty()) throw new GlobalException(ResultCode.NOT_FOUND_DEVICE_NAME);
        BLEDevice bleDevice = deviceList.get(0);
        double ratio = (double) updateBLEReq.getLastCount() /bleDevice.getCapacity();
        BLEstatus status;
        if (ratio < 0.3) status = BLEstatus.VACANT;
        else if (ratio < 0.7) status = BLEstatus.AVAILABLE;
        else status = BLEstatus.CROWDED;

        BLEData bleData = new BLEData();
        bleData.setDevice(bleDevice);
        bleData.setLastCount(updateBLEReq.getLastCount());
        bleData.setLastStatus(status);
        bleData.setLastTime(updateBLEReq.getLastTime());
        bleDataRepository.save(bleData);

        return new UpdateBLERes(bleData);
    }

    @Transactional(readOnly = true)
    public GetBLERes getBLE(Long placeId) {
        LocalDateTime now = LocalDateTime.now();
        Place place = placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
        List<BLEDevice> devices = bledeviceRepository.findByPlace(place);
        if (devices.isEmpty()) throw new GlobalException(ResultCode.NOT_FOUND_DEVICE);
        BLEDevice device = devices.get(0);
        BLEData latest = bleDataRepository.findTopByDeviceOrderByLastTimeDesc(device).orElseThrow(() -> new GlobalException(ResultCode.NO_DATA_FOR_DEVICE));
        BLEstatus status;
        if (latest.getLastTime() == null ||
                Duration.between(latest.getLastTime(), LocalDateTime.now()).toMinutes() >= 30) {
            status = BLEstatus.FAILURE;
        }
        else status = latest.getLastStatus();
        return new GetBLERes(device, latest, status);
    }

}
