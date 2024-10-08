package devkor.com.teamcback.domain.building.service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING_NICKNAME;

import devkor.com.teamcback.domain.building.dto.request.SaveBuildingNicknameReq;
import devkor.com.teamcback.domain.building.dto.response.DeleteBuildingNicknameRes;
import devkor.com.teamcback.domain.building.dto.response.GetBuildingNicknameListRes;
import devkor.com.teamcback.domain.building.dto.response.GetBuildingNicknameRes;
import devkor.com.teamcback.domain.building.dto.response.SaveBuildingNicknameRes;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import devkor.com.teamcback.domain.building.repository.BuildingNicknameRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
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

    // 건물 별명 저장
    @Transactional
    public SaveBuildingNicknameRes saveBuildingNickname(Long buildingId, SaveBuildingNicknameReq req) {
        Building building = findBuilding(buildingId);
        BuildingNickname buildingNickname = new BuildingNickname(building, req.getNickname());

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

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private BuildingNickname findBuildingNickname(Long buildingNicknameId) {
        return buildingNicknameRepository.findById(buildingNicknameId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING_NICKNAME));
    }
}
