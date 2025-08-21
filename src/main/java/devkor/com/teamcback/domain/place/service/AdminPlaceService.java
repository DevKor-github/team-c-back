package devkor.com.teamcback.domain.place.service;

import devkor.com.teamcback.domain.building.dto.response.SaveBuildingMainImageRes;
import devkor.com.teamcback.domain.common.util.FileUtil;
import devkor.com.teamcback.domain.place.dto.request.CreatePlaceReq;
import devkor.com.teamcback.domain.place.dto.request.ModifyPlaceReq;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.place.dto.response.CreatePlaceRes;
import devkor.com.teamcback.domain.place.dto.response.DeletePlaceRes;
import devkor.com.teamcback.domain.place.dto.response.GetPlaceListRes;
import devkor.com.teamcback.domain.place.dto.response.GetPlaceRes;
import devkor.com.teamcback.domain.place.dto.response.ModifyPlaceRes;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.routes.entity.Node;
import devkor.com.teamcback.domain.routes.repository.NodeRepository;
import devkor.com.teamcback.domain.suggestion.dto.response.SavePlaceImageRes;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.infra.s3.FilePath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Service
@RequiredArgsConstructor
public class AdminPlaceService {
    private final BuildingRepository buildingRepository;
    private final PlaceRepository placeRepository;
    private final NodeRepository nodeRepository;
    private final FileUtil fileUtil;

    // 건물 id와 층에 해당하는 장소 리스트 조회
    @Transactional(readOnly = true)
    public GetPlaceListRes getPlaceList(Long buildingId, Double floor) {
        Building building = findBuilding(buildingId);
        List<GetPlaceRes> placeResList = new ArrayList<>();
        List<Place> placeList = placeRepository.findAllByBuildingAndFloor(building, floor);
        for (Place place : placeList) {
            String imageUrl = null;
            if(place.getFileUuid() != null) {
                imageUrl = fileUtil.getThumbnail(place.getFileUuid());
            }

            placeResList.add(new GetPlaceRes(place, imageUrl));
        }


        if(placeResList.isEmpty()) throw new GlobalException(NOT_FOUND_PLACE);

        return new GetPlaceListRes(placeResList);
    }

    // 장소 생성
    @Transactional
    public CreatePlaceRes createPlace(CreatePlaceReq req) {
        Building building = findBuilding(req.getBuildingId());
        Node node = findNode(req.getNodeId());
        Place place = placeRepository.save(new Place(req, building, node));
        return new CreatePlaceRes(place);
    }

    // 장소 수정
    @Transactional
    public ModifyPlaceRes modifyPlace(Long placeId, ModifyPlaceReq req) {
        Building building = findBuilding(req.getBuildingId());
        Node node = findNode(req.getNodeId());
        Place place = findPlace(placeId);
        place.update(req, building, node);
        return new ModifyPlaceRes();
    }

    // 장소 삭제
    @Transactional
    public DeletePlaceRes deletePlace(Long facilityId) {
        Place place = findPlace(facilityId);
        placeRepository.delete(place);

        return new DeletePlaceRes();
    }

    // 장소 사진 저장
    @Transactional
    public SavePlaceImageRes savePlaceImage(Long placeId, List<MultipartFile> images) {

        Place place = findPlace(placeId);

        if(place.getFileUuid() == null) {
            place.setFileUuid(fileUtil.createFileUuid());
        }

        fileUtil.upload(images, place.getFileUuid(), null, FilePath.PLACE);

        return new SavePlaceImageRes();
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private Node findNode(Long nodeId) {
        return nodeRepository.findById(nodeId).orElseThrow(() -> new GlobalException(NOT_FOUND_NODE));
    }

    private Place findPlace(Long facilityId) {
        return placeRepository.findById(facilityId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
    }

}
