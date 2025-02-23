package devkor.com.teamcback.domain.place.service;

import devkor.com.teamcback.domain.place.dto.request.SavePlaceNicknameReq;
import devkor.com.teamcback.domain.place.dto.response.*;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceNickname;
import devkor.com.teamcback.domain.place.repository.PlaceNicknameRepository;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.search.util.HangeulUtils;
import devkor.com.teamcback.global.exception.exception.GlobalException;
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
    private final HangeulUtils hangeulUtils;

    // 강의실 별명 저장
    @Transactional
    public SavePlaceNicknameRes saveClassroomNickname(Long placeId, SavePlaceNicknameReq req) {
        Place place = findPlace(placeId);
        String nickname = req.getNickname().replace(" ", "");
        PlaceNickname placeNickname = new PlaceNickname(place, nickname, hangeulUtils.extractChosung(nickname), hangeulUtils.decomposeHangulString(nickname));

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

    /**
     * Place Nickname Tables 업데이트
     */
    @Transactional
    public UpdatePlaceNicknamesRes updatePlaceNicknames() {
        // 닉네임 자소분리/초성분리 로직
        List<PlaceNickname> nonDecomposedPNicknames = placeNicknameRepository.findByChosungIsNullOrJasoDecomposeIsNull();

        for (PlaceNickname p : nonDecomposedPNicknames) {
            String nickname = p.getNickname();
            p.update(hangeulUtils.extractChosung(nickname), hangeulUtils.decomposeHangulString(nickname));
        }
        placeNicknameRepository.saveAll(nonDecomposedPNicknames);

        // 닉네임 공백제거 로직
        List<PlaceNickname> blankPNicknames = placeNicknameRepository.findAllByNicknameContaining(" ");

        for (PlaceNickname p : blankPNicknames) {
            String nickname = p.getNickname().replace(" ", "");
            p.update(nickname, hangeulUtils.extractChosung(nickname), hangeulUtils.decomposeHangulString(nickname));
        }
        placeNicknameRepository.saveAll(blankPNicknames);

        return new UpdatePlaceNicknamesRes(nonDecomposedPNicknames.size() + blankPNicknames.size());
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
    }

    private PlaceNickname findPlaceNickname(Long nicknameId) {
        return placeNicknameRepository.findById(nicknameId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE_NICKNAME));
    }
}
