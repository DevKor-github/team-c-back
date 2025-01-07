package devkor.com.teamcback.domain.building.service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING_NICKNAME;

import devkor.com.teamcback.domain.building.dto.request.SaveBuildingNicknameReq;
import devkor.com.teamcback.domain.building.dto.response.*;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import devkor.com.teamcback.domain.building.repository.BuildingNicknameRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.search.util.HangeulUtils;
import devkor.com.teamcback.global.exception.GlobalException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminBuildingNicknameService {
    private final BuildingNicknameRepository buildingNicknameRepository;
    private final BuildingRepository buildingRepository;
    private final HangeulUtils hangeulUtils;

    // 건물 별명 저장
    @Transactional
    public SaveBuildingNicknameRes saveBuildingNickname(Long buildingId, SaveBuildingNicknameReq req) {
        Building building = findBuilding(buildingId);
        String nickname = req.getNickname();
        BuildingNickname buildingNickname = new BuildingNickname(building, nickname, hangeulUtils.extractChosung(nickname), hangeulUtils.decomposeHangulString(nickname));

        buildingNicknameRepository.save(buildingNickname);

        return new SaveBuildingNicknameRes();
    }

    // 건물 별명 삭제
    @Transactional
    public DeleteBuildingNicknameRes deleteBuildingNickname(Long nicknameId) {
        BuildingNickname buildingNickname = findBuildingNickname(nicknameId);

        buildingNicknameRepository.delete(buildingNickname);

        return new DeleteBuildingNicknameRes();
    }

    // 건물 별명 조회
    @Transactional(readOnly = true)
    public GetBuildingNicknameListRes getBuildingNickname(Long buildingId) {
        Building building = findBuilding(buildingId);
        List<GetBuildingNicknameRes> nicknameList = buildingNicknameRepository.findAllByBuilding(building)
            .stream().map(buildingNickname -> new GetBuildingNicknameRes(buildingNickname)).toList();


        return new GetBuildingNicknameListRes(building, nicknameList);
    }

    // 건물 별명 테이블 업데이트
    @Transactional
    public UpdateBuildingNicknamesRes updateBuildingNicknames() {
        List<BuildingNickname> buildingNicknames = buildingNicknameRepository.findByChosungIsNullOrJasoDecomposeIsNull();

        for (BuildingNickname b : buildingNicknames) {
            String nickname = b.getNickname();
            b.update(hangeulUtils.extractChosung(nickname), hangeulUtils.decomposeHangulString(nickname));
        }
        buildingNicknameRepository.saveAll(buildingNicknames);

        return new UpdateBuildingNicknamesRes(buildingNicknames.size());
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private BuildingNickname findBuildingNickname(Long buildingNicknameId) {
        return buildingNicknameRepository.findById(buildingNicknameId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING_NICKNAME));
    }
}
