package devkor.com.teamcback.domain.ble.service;

import devkor.com.teamcback.domain.ble.dto.request.CreateBLEDeviceReq;
import devkor.com.teamcback.domain.ble.dto.request.ModifyBLEDeviceReq;
import devkor.com.teamcback.domain.ble.dto.response.CreateBLEDeviceRes;
import devkor.com.teamcback.domain.ble.dto.response.DeleteBLEDeviceRes;
import devkor.com.teamcback.domain.ble.dto.response.ModifyBLEDeviceRes;
import devkor.com.teamcback.domain.ble.entity.BLEDevice;
import devkor.com.teamcback.domain.ble.repository.BLEDeviceRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminBLEService {
    private final BLEDeviceRepository bleDeviceRepository;
    private final PlaceRepository placeRepository;

    //bledevice 생성
    @Transactional
    public CreateBLEDeviceRes createBLEDevice(CreateBLEDeviceReq createBLEDeviceReq) {
        Place place = placeRepository.findById(createBLEDeviceReq.getPlaceId()).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_PLACE));
        if (bleDeviceRepository.existsByDeviceName(createBLEDeviceReq.getDeviceName())) throw new GlobalException(ResultCode.EXISTING_DEVICE_NAME);
        if (bleDeviceRepository.existsByPlace(place)) throw new GlobalException(ResultCode.EXISTING_PLACE_FOR_DEVICE);
        BLEDevice bleDevice = bleDeviceRepository.save(new BLEDevice(createBLEDeviceReq, place));
        return new CreateBLEDeviceRes(bleDevice);
    }

    //bledevice 수정
    @Transactional
    public ModifyBLEDeviceRes modifyBLEDevice(ModifyBLEDeviceReq modifyBLEReq){
        Place place = placeRepository.findById(modifyBLEReq.getPlaceId()).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_PLACE));
        BLEDevice bleDevice = bleDeviceRepository.findById(modifyBLEReq.getId()).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DEVICE));
        if (bleDeviceRepository.existsByDeviceName(modifyBLEReq.getDeviceName()) && !Objects.equals(modifyBLEReq.getDeviceName(), bleDevice.getDeviceName())) throw new GlobalException(ResultCode.EXISTING_DEVICE_NAME);
        if (bleDeviceRepository.existsByPlace(place) && !Objects.equals(modifyBLEReq.getPlaceId(), bleDevice.getPlace().getId())) throw new GlobalException(ResultCode.EXISTING_PLACE_FOR_DEVICE);
        bleDevice.update(modifyBLEReq, place);
        return new ModifyBLEDeviceRes();
    }

    //bledevice 삭제
    @Transactional
    public DeleteBLEDeviceRes deleteBLEDevice(Long bleId) {
        BLEDevice bleDevice = bleDeviceRepository.findById(bleId).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DEVICE));
        bleDeviceRepository.delete(bleDevice);
        return new DeleteBLEDeviceRes();
    }
}
