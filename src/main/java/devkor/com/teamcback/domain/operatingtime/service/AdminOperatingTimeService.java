package devkor.com.teamcback.domain.operatingtime.service;

import devkor.com.teamcback.domain.operatingtime.dto.request.SavePlaceOperatingTimeReq;
import devkor.com.teamcback.domain.operatingtime.dto.response.GetPlaceOperatingTimeRes;
import devkor.com.teamcback.domain.operatingtime.dto.response.SavePlaceOperatingTimeRes;
import devkor.com.teamcback.domain.operatingtime.repositoy.OperatingTimeRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOperatingTimeService {
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

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_PLACE));
    }

}
