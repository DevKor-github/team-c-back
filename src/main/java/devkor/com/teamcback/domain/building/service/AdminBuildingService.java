package devkor.com.teamcback.domain.building.service;

import devkor.com.teamcback.domain.building.dto.response.SaveBuildingImageRes;
import devkor.com.teamcback.domain.building.dto.response.SaveBuildingMainImageRes;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingImage;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.common.repository.FileRepository;
import devkor.com.teamcback.domain.common.util.FileUtil;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.infra.s3.FilePath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static devkor.com.teamcback.global.response.ResultCode.INCORRECT_FLOOR;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING;

@Service
@RequiredArgsConstructor
public class AdminBuildingService {

    private final BuildingRepository buildingRepository;
    private final FileUtil fileUtil;

    // 건물 내부 사진 저장
    @Transactional
    public SaveBuildingMainImageRes saveBuildingMainImage(Long buildingId, MultipartFile image) {

        Building building = findBuilding(buildingId);

        if(building.getFileUuid() == null) {
            building.setFileUuid(fileUtil.createFileUuid());
        }

        fileUtil.upload(image, building.getFileUuid(), FilePath.BUILDING, 1L);

        return new SaveBuildingMainImageRes();
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }
}
