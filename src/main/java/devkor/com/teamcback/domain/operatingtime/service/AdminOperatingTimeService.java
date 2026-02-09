package devkor.com.teamcback.domain.operatingtime.service;

import devkor.com.teamcback.domain.operatingtime.dto.request.SavePlaceOperatingTimeConditionReq;
import devkor.com.teamcback.domain.operatingtime.dto.request.SavePlaceOperatingTimeConditionTimeReq;
import devkor.com.teamcback.domain.operatingtime.dto.request.SavePlaceOperatingTimeReq;
import devkor.com.teamcback.domain.operatingtime.dto.response.GetPlaceOperatingTimeRes;
import devkor.com.teamcback.domain.operatingtime.dto.response.SavePlaceOperatingTimeConditionRes;
import devkor.com.teamcback.domain.operatingtime.dto.response.SavePlaceOperatingTimeRes;
import devkor.com.teamcback.domain.operatingtime.entity.DayOfWeek;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingCondition;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingTime;
import devkor.com.teamcback.domain.operatingtime.repositoy.OperatingConditionRepository;
import devkor.com.teamcback.domain.operatingtime.repositoy.OperatingTimeRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOperatingTimeService {
    private final OperatingConditionRepository operatingConditionRepository;
    private final OperatingTimeRepository operatingTimeRepository;
    private final PlaceRepository placeRepository;

    @Transactional(readOnly = true)
    public GetPlaceOperatingTimeRes getPlaceOperatingTime(Long placeId) {
        Place place = findPlace(placeId);

        return new GetPlaceOperatingTimeRes(place);
    }

    @Transactional
    public SavePlaceOperatingTimeRes savePlaceOperatingTime(Long placeId, SavePlaceOperatingTimeReq req) {
        Place place = findPlace(placeId);

        // 운영시간 수정
        place.updateOperatingTime(req);

        return new SavePlaceOperatingTimeRes();
    }

    @Transactional
    public SavePlaceOperatingTimeConditionRes savePlaceOperatingTimeCondition(Long placeId, SavePlaceOperatingTimeConditionReq req) {
        Place place = findPlace(placeId);

        // 운영조건 찾기
        OperatingCondition operatingCondition = findOperatingConditionOfPlace(req.getDayOfWeek(), req.getIsHoliday(), req.getIsVacation(), req.getIsEvenWeek(), place);

        // 기존 운영조건이 존재하면 해당하는 운영시간 모두 삭제
        if(operatingCondition != null) {
            operatingTimeRepository.deleteAllByOperatingCondition(operatingCondition);
        }
        // 존재하지 않으면 새로 생성
        else {
            operatingCondition = operatingConditionRepository.save(new OperatingCondition(req));
        }

        // 운영시간 저장
        for(SavePlaceOperatingTimeConditionTimeReq timeReq : req.getTimeList()) {
            LocalTime startTime = LocalTime.of(timeReq.getStartHour(), timeReq.getStartMinute());
            LocalTime endTime = LocalTime.of(timeReq.getEndHour(), timeReq.getEndMinute());

            operatingTimeRepository.save(new OperatingTime(operatingCondition, startTime, endTime));
        }

        return new SavePlaceOperatingTimeConditionRes();
    }

    /**
     * 장소 id에 해당하는 장소 찾기
     */
    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_PLACE));
    }

    /**
     * 해당 장소와 조건에 맞는 운영 조건 찾기
     */
    private OperatingCondition findOperatingConditionOfPlace(DayOfWeek dayOfWeek, Boolean isHoliday, Boolean isVacation, Boolean isEvenWeek, Place place) {
        OperatingCondition operatingCondition  = operatingConditionRepository.findByDayOfWeekAndIsHolidayAndIsVacationAndPlace(dayOfWeek, isHoliday, isVacation, place);

        if(operatingCondition == null) return null;

        if(dayOfWeek == DayOfWeek.SATURDAY) { // 토요일인 경우
            if(operatingCondition.getIsEvenWeek() == null || operatingCondition.getIsEvenWeek() == isEvenWeek) return operatingCondition;
            else return null;
        }

        return operatingCondition;
    }
}
