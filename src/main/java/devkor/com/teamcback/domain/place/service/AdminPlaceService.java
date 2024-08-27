package devkor.com.teamcback.domain.place.service;

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
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Service
@RequiredArgsConstructor
public class AdminPlaceService {
    private final BuildingRepository buildingRepository;
    private final PlaceRepository placeRepository;
    private final NodeRepository nodeRepository;

    // 건물 id와 층에 해당하는 장소 리스트 조회
    @Transactional(readOnly = true)
    public GetPlaceListRes getPlaceList(Long buildingId, Double floor) {
        Building building = findBuilding(buildingId);
        List<GetPlaceRes> placeList = placeRepository.findAllByBuildingAndFloor(building, floor).stream().map(GetPlaceRes::new).collect(Collectors.toList());;
        if(placeList.isEmpty()) throw new GlobalException(NOT_FOUND_PLACE);

        return new GetPlaceListRes(placeList);
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
