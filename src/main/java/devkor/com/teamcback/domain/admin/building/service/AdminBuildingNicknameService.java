package devkor.com.teamcback.domain.admin.building.service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING;

import devkor.com.teamcback.domain.admin.building.dto.request.SaveBuildingNicknameReq;
import devkor.com.teamcback.domain.admin.building.dto.response.SaveBuildingNicknameRes;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import devkor.com.teamcback.domain.building.repository.BuildingNicknameRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.global.exception.GlobalException;
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

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }
}
