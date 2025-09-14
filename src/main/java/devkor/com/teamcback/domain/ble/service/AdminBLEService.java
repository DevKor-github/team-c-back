package devkor.com.teamcback.domain.ble.service;

import devkor.com.teamcback.domain.ble.dto.request.CreateBLEReq;
import devkor.com.teamcback.domain.ble.dto.response.CreateBLERes;
import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import devkor.com.teamcback.domain.ble.repository.BLEDeviceRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_PLACE;

@Service
@RequiredArgsConstructor
public class AdminBLEService {
    private final BLEDeviceRepository bleDeviceRepository;
    private final PlaceRepository placeRepository;

    @Transactional
    public CreateBLERes CreateBLEDevice(CreateBLEReq createBLEReq) {
        Place place = placeRepository.findById(createBLEReq.getPlaceId()).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
        if (bleDeviceRepository.existsByDeviceName(createBLEReq.getDeviceName())) throw new GlobalException(ResultCode.EXISTING_DEVICE_NAME);
        BLEDevice bleDevice = bleDeviceRepository.save(new BLEDevice(createBLEReq.getDeviceName(), place, createBLEReq.getCapacity()));
        return new CreateBLERes(bleDevice);
    }
}
