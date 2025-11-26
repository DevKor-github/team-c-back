package devkor.com.teamcback.domain.ble.service;


import devkor.com.teamcback.domain.ble.dto.request.UpdateBLEReq;
import devkor.com.teamcback.domain.ble.dto.response.BLETimePatternRes;
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

    // 평균 구해올 시간대 라벨
    private static final int[] TIME_SLOTS = {7, 10, 13, 16, 19, 22};
    //요일 라벨 (1=월요일, 7=일요일)
    private static final int[] DAY_OF_WEEKS = {1, 2, 3, 4, 5, 6, 7};

    @Transactional
    public UpdateBLERes updateBLE(UpdateBLEReq updateBLEReq) {
        BLEDevice bleDevice = bledeviceRepository.findByDeviceName(updateBLEReq.getDeviceName());
        int capacity = bleDevice.getCapacity();
        int people = getBlEPeople(updateBLEReq.getLastCount(), bleDevice);
        double final_ratio = (double) people / capacity;
        BLEstatus status;
        if (final_ratio < 0.3) status = BLEstatus.VACANT;
        else if (final_ratio < 0.7) status = BLEstatus.AVAILABLE;
        else status = BLEstatus.CROWDED;

        BLEData bleData = new BLEData();
        bleData.setDevice(bleDevice);
        bleData.setLastCount(updateBLEReq.getLastCount());
        bleData.setLastStatus(status);
        bleData.setLastTime(updateBLEReq.getLastTime());
        bleDataRepository.save(bleData);

        return new UpdateBLERes(bleData);
    }

    private int getBlEPeople(int lastCount, BLEDevice bleDevice) {
        if (bleDevice == null) throw new GlobalException(ResultCode.NOT_FOUND_DEVICE_NAME);
        double ratio = bleDevice.getRatio();
        double defaultCount = bleDevice.getDefaultCount();
        return calculate_people(lastCount, ratio, defaultCount);
    }

    //count, ratio, defaultcount로 예측 인원 계산(int)
    private int calculate_people(int count, double ratio, double defaultCount) {
        return (int) Math.round(count * ratio + defaultCount);
    }

    @Transactional(readOnly = true)
    public GetBLERes getBLE(Long placeId) {
        Place place = placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
        BLEDevice device = bledeviceRepository.findByPlace(place);
        if (device == null) throw new GlobalException(ResultCode.NOT_FOUND_DEVICE);
        BLEData latest = bleDataRepository.findTopByDeviceOrderByLastTimeDesc(device).orElseThrow(() -> new GlobalException(ResultCode.NO_DATA_FOR_DEVICE));
        BLEstatus status;
        if (latest.getLastTime() == null ||
                Duration.between(latest.getLastTime(), LocalDateTime.now()).toMinutes() >= 30) {
            status = BLEstatus.FAILURE;
        }
        else status = latest.getLastStatus();
        // 사람 수를 예측 후 10의 배수로 리턴
        int people = getBlEPeople(latest.getLastCount(), device);
        people = (int) Math.round(people / 10.0) * 10;
        return new GetBLERes(device, latest, status, people);
    }

    @Transactional(readOnly = true)
    public BLETimePatternRes getBLETimePattern(Long placeId) {
        //place, device 조회
        Place place = placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
        BLEDevice device = bledeviceRepository.findByPlace(place);
        if (device == null) throw new GlobalException(ResultCode.NOT_FOUND_DEVICE);
        double ratio = device.getRatio();
        double defaultCount = device.getDefaultCount();
        //기간 설정: 1달
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMonths(1);
        //1달치 bledata 가져오기
        List<BLEData> dataList = bleDataRepository.findAllByDeviceAndLastTimeBetweenOrderByLastTimeAsc(device, start, end);
        //계산 위한 배열 준비
        // dayIndex: 0~6 (월~일), timeIndex: 0~6 (7,10,13,16,19,22)
        long[][] sum = new long[DAY_OF_WEEKS.length][TIME_SLOTS.length];
        long[][] count = new long[DAY_OF_WEEKS.length][TIME_SLOTS.length];
        // 시간대를 "가장 가까운 target 시간"으로 매핑하기 위해 분 단위로 처리
        int[] slotMinutes = new int[TIME_SLOTS.length];
        for (int i = 0; i < TIME_SLOTS.length; i++) {
            slotMinutes[i] = TIME_SLOTS[i] * 60;
        }
        // BLEData를 요일/시간대 bucket에 넣어서 합/개수 집계
        for (BLEData d : dataList) {
            if (d.getLastTime() == null) continue;
            if (d.getLastCount() == null) continue;
            LocalDateTime t = d.getLastTime();
            // 요일 index (0~6) : DayOfWeek.getValue() 는 1~7(MON~SUN)
            int dayValue = t.getDayOfWeek().getValue(); // 1~7
            int dayIndex = dayValue - 1;
            // 분 단위 시간 (0~1439)
            int minuteOfDay = t.getHour() * 60 + t.getMinute();
            // 이 데이터가 들어갈 slot 찾기 (전후 30분 이내만 허용)
            int bestSlotIndex = -1;
            int bestDiff = Integer.MAX_VALUE;

            for (int i = 0; i < slotMinutes.length; i++) {
                int diff = Math.abs(minuteOfDay - slotMinutes[i]);

                // 전후 30분(= 60분 window) 안에 들어오는 경우만 고려
                if (diff <= 30 && diff < bestDiff) {
                    bestDiff = diff;
                    bestSlotIndex = i;
                }
            }
            // 어떤 slot에도 해당되지 않으면 이 데이터는 버림
            if (bestSlotIndex == -1) {
                continue;
            }
            // 합/개수 축적
            sum[dayIndex][bestSlotIndex] += calculate_people(d.getLastCount(), ratio, defaultCount);
            count[dayIndex][bestSlotIndex] += 1;
        }
        // 평균 계산 (반올림 후 int)
        int[][] averages = new int[DAY_OF_WEEKS.length][TIME_SLOTS.length];
        for (int di = 0; di < DAY_OF_WEEKS.length; di++) {
            for (int ti = 0; ti < TIME_SLOTS.length; ti++) {
                if (count[di][ti] == 0L) {
                    averages[di][ti] = 0; // 데이터 없으면 0
                } else {
                    double avg = (double) sum[di][ti] / (double) count[di][ti];
                    averages[di][ti] = (int) Math.round(avg);
                }
            }
        }
        return new BLETimePatternRes(
                placeId,
                TIME_SLOTS,
                DAY_OF_WEEKS,
                averages
        );
    }

}
