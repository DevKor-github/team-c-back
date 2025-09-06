package devkor.com.teamcback.domain.ble.service;


import devkor.com.teamcback.domain.ble.dto.request.UpdateBLEReq;
import devkor.com.teamcback.domain.ble.dto.response.GetBLERes;
import devkor.com.teamcback.domain.ble.dto.response.UpdateBLERes;
import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import devkor.com.teamcback.domain.ble.entity.BLEstatus;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class BLEService {
    private final BLEDeviceRepository blEdeviceRepository;
    private final PlaceRepository placeRepository;

    @Transactional
    public UpdateBLERes updateBLEDevice(UpdateBLEReq updateBLEReq) {
        List<BLEDevice> deviceList = blEdeviceRepository.findByDeviceName(updateBLEReq.getDeviceName());
        if (deviceList.isEmpty()) throw new GlobalException(ResultCode.NOT_FOUND_DEVICE_NAME);
        BLEDevice bleDevice = deviceList.get(0);
        double ratio = (double) updateBLEReq.getLastCount() /bleDevice.getCapacity();
        BLEstatus status;
        if (ratio < 0.3) status = BLEstatus.VACANT;
        else if (ratio < 0.7) status = BLEstatus.AVAILABLE;
        else status = BLEstatus.CROWDED;
        bleDevice.update(updateBLEReq, status);
        return new UpdateBLERes(bleDevice);
    }

    @Transactional(readOnly = true)
    public GetBLERes getBLE(Long placeId) {
        LocalDateTime now = LocalDateTime.now();
        Place place = placeRepository.findById(placeId).orElse(null);
        List<BLEDevice> devices = blEdeviceRepository.findByPlace(place);
        if (devices.isEmpty()) throw new GlobalException(ResultCode.NOT_FOUND_DEVICE);
        BLEDevice device = devices.get(0);
        if (Duration.between(device.getLastTime(),now).toMinutes() >= 30) {
            device.update(BLEstatus.FAILURE);
        };
        return new GetBLERes(device);
    }

}
