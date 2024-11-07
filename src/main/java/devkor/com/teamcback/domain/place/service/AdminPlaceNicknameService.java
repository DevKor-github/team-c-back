package devkor.com.teamcback.domain.place.service;

import devkor.com.teamcback.domain.place.dto.request.SavePlaceNicknameReq;
import devkor.com.teamcback.domain.place.dto.response.DeletePlaceNicknameRes;
import devkor.com.teamcback.domain.place.dto.response.GetClassroomNicknameRes;
import devkor.com.teamcback.domain.place.dto.response.GetPlaceNicknameListRes;
import devkor.com.teamcback.domain.place.dto.response.SavePlaceNicknameRes;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceNickname;
import devkor.com.teamcback.domain.place.repository.PlaceNicknameRepository;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.search.HangulUtils;
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_PLACE;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_PLACE_NICKNAME;

@Service
@RequiredArgsConstructor
public class AdminPlaceNicknameService {
    private final PlaceNicknameRepository placeNicknameRepository;
    private final PlaceRepository placeRepository;
    private final HangulUtils hangulUtils;

    // 강의실 별명 저장
    @Transactional
    public SavePlaceNicknameRes saveClassroomNickname(Long placeId, SavePlaceNicknameReq req) {
        Place place = findPlace(placeId);
        String nickname = req.getNickname();
        PlaceNickname placeNickname = new PlaceNickname(place, nickname, hangulUtils.extractChosung(nickname), hangulUtils.decomposeHangulString(nickname));

        placeNicknameRepository.save(placeNickname);

        return new SavePlaceNicknameRes();
    }

    // 강의실 별명 삭제
    @Transactional
    public DeletePlaceNicknameRes deleteClassroomNickname(Long nicknameId) {
        PlaceNickname placeNickname = findPlaceNickname(nicknameId);

        placeNicknameRepository.delete(placeNickname);

        return new DeletePlaceNicknameRes();
    }

    // 강의실 별명 조회
    @Transactional(readOnly = true)
    public GetPlaceNicknameListRes getClassroomNickname(Long classroomId) {
        Place place = findPlace(classroomId);
        List<GetClassroomNicknameRes> nicknameList = placeNicknameRepository.findAllByPlace(place)
            .stream().map(placeNickname -> new GetClassroomNicknameRes(placeNickname)).toList();


        return new GetPlaceNicknameListRes(place, nicknameList);
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
    }

    private PlaceNickname findPlaceNickname(Long nicknameId) {
        return placeNicknameRepository.findById(nicknameId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE_NICKNAME));
    }
}
