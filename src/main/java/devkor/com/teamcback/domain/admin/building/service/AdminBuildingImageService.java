package devkor.com.teamcback.domain.admin.building.service;

import static devkor.com.teamcback.global.response.ResultCode.DUPLICATED_BUILDING_IMAGE;
import static devkor.com.teamcback.global.response.ResultCode.INCORRECT_FLOOR;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING_IMAGE;

import devkor.com.teamcback.domain.admin.building.dto.response.DeleteBuildingImageRes;
import devkor.com.teamcback.domain.admin.building.dto.response.GetBuildingImageRes;
import devkor.com.teamcback.domain.admin.building.dto.response.ModifyBuildingImageRes;
import devkor.com.teamcback.domain.admin.building.dto.response.SaveBuildingImageRes;
import devkor.com.teamcback.domain.admin.building.dto.response.SearchBuildingImageRes;
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
public class AdminBuildingImageService {
    private final BuildingRepository buildingRepository;
    private final BuildingImageRepository buildingImageRepository;
    private final S3Util s3Util;

    // 건물 내부 사진 저장
    @Transactional
    public SaveBuildingImageRes saveBuildingImage(Long buildingId, Double floor, MultipartFile image) {
        String imageUrl = s3Util.uploadFile(image, FilePath.BUILDING_IMAGE);
        Building building = findBuilding(buildingId);

        if(floor < building.getUnderFloor() * (-1) || floor > building.getFloor()) {
            throw new GlobalException(INCORRECT_FLOOR);
        }
        checkExistedImage(building, floor); // 해당 건물, 층에 해당하는 사진이 있는지 확인
        BuildingImage buildingImage = buildingImageRepository.save(new BuildingImage(floor, imageUrl, building));

        return new SaveBuildingImageRes(buildingImage);
    }

    // 건물 내부 사진 수정
    @Transactional
    public ModifyBuildingImageRes modifyBuildingImage(Long buildingImageId, MultipartFile image) {
        BuildingImage buildingImage = findBuildingImage(buildingImageId);
        if(s3Util.exists(buildingImage.getImage(), FilePath.BUILDING_IMAGE)) {
            s3Util.deleteFile(buildingImage.getImage(), FilePath.BUILDING_IMAGE); // 기존 사진 S3에서 삭제
        }

        String imageUrl = s3Util.uploadFile(image, FilePath.BUILDING_IMAGE);
        buildingImage.update(imageUrl);

        return new ModifyBuildingImageRes(buildingImage);
    }

    // 건물 내부 사진 삭제
    @Transactional
    public DeleteBuildingImageRes deleteBuildingImage(Long buildingImageId) {
        BuildingImage buildingImage = findBuildingImage(buildingImageId);
        if(s3Util.exists(buildingImage.getImage(), FilePath.BUILDING_IMAGE)) {
            s3Util.deleteFile(buildingImage.getImage(), FilePath.BUILDING_IMAGE); // 기존 사진 S3에서 삭제
        }
        buildingImageRepository.delete(buildingImage);

        return new DeleteBuildingImageRes();
    }

    // 건물 내부 사진 검색
    @Transactional(readOnly = true)
    public SearchBuildingImageRes searchBuildingImage(Long buildingId, Double floor) {
        Building building = findBuilding(buildingId);
        BuildingImage buildingImage = findBuildingImage(building, floor);

        return new SearchBuildingImageRes(buildingImage);
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private BuildingImage findBuildingImage(Long buildingImageId) {
        return buildingImageRepository.findById(buildingImageId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING_IMAGE));
    }

    private BuildingImage findBuildingImage(Building building, Double floor) {
        BuildingImage buildingImage = buildingImageRepository.findByBuildingAndFloor(building, floor);
        if(buildingImage == null) {
            throw new GlobalException(NOT_FOUND_BUILDING_IMAGE);
        }
        return buildingImage;

    }

    private void checkExistedImage(Building building, Double floor) {
        BuildingImage buildingImage = buildingImageRepository.findByBuildingAndFloor(building, floor);
        if(buildingImage != null) {
            throw new GlobalException(DUPLICATED_BUILDING_IMAGE);
        }
    }
}
