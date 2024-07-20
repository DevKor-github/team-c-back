package devkor.com.teamcback.domain.admin.service;

import static devkor.com.teamcback.global.response.ResultCode.DUPLICATED_BUILDING_IMAGE;
import static devkor.com.teamcback.global.response.ResultCode.INCORRECT_FLOOR;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING;

import devkor.com.teamcback.domain.admin.dto.request.SaveBuildingImageReq;
import devkor.com.teamcback.domain.admin.dto.response.SaveBuildingImageRes;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingImage;
import devkor.com.teamcback.domain.building.repository.BuildingImageRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import devkor.com.teamcback.infra.s3.FilePath;
import devkor.com.teamcback.infra.s3.S3Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AdminBuildingService {
    private final BuildingRepository buildingRepository;
    private final BuildingImageRepository buildingImageRepository;
    private final S3Util s3Util;

    @Transactional
    public SaveBuildingImageRes saveBuildingImage(SaveBuildingImageReq req, MultipartFile image) {
        String imageUrl = s3Util.uploadFile(image, FilePath.BUILDING_IMAGE);
        Building building = findBuilding(req.getBuildingId());

        if(req.getFloor() < building.getUnderFloor() || req.getFloor() > building.getFloor()) {
            throw new GlobalException(INCORRECT_FLOOR);
        }
        checkExistedImage(building, req.getFloor()); // 해당 건물, 층에 해당하는 사진이 있는지 확인
        BuildingImage buildingImage = buildingImageRepository.save(new BuildingImage(req, imageUrl, building));

        return new SaveBuildingImageRes(buildingImage);
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private void checkExistedImage(Building building, Double floor) {
        BuildingImage buildingImage = buildingImageRepository.findByBuildingAndFloor(building, floor);
        if(buildingImage != null) {
            throw new GlobalException(DUPLICATED_BUILDING_IMAGE);
        }
    }
}
