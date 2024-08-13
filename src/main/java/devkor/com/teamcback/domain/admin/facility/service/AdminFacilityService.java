package devkor.com.teamcback.domain.admin.facility.service;

import devkor.com.teamcback.domain.admin.facility.dto.request.CreateFacilityReq;
import devkor.com.teamcback.domain.admin.facility.dto.request.ModifyFacilityReq;
import devkor.com.teamcback.domain.admin.facility.dto.response.*;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.repository.FacilityRepository;
import devkor.com.teamcback.domain.navigate.entity.Node;
import devkor.com.teamcback.domain.navigate.repository.NodeRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Service
@RequiredArgsConstructor
public class AdminFacilityService {
    private final BuildingRepository buildingRepository;
    private final FacilityRepository facilityRepository;
    private final NodeRepository nodeRepository;

    // 건물 id와 층에 해당하는 편의시설 리스트 조회
    @Transactional(readOnly = true)
    public GetFacilityListRes getFacilityList(Long buildingId, Double floor) {
        Building building = findBuilding(buildingId);
        List<GetFacilityRes> facilityList = facilityRepository.findAllByBuildingAndFloor(building, floor).stream().map(GetFacilityRes::new).collect(Collectors.toList());;
        if(facilityList.isEmpty()) throw new GlobalException(NOT_FOUND_FACILITY);

        return new GetFacilityListRes(facilityList);
    }

    // 편의시설 생성
    @Transactional
    public CreateFacilityRes createFacility(CreateFacilityReq req) {
        Building building = findBuilding(req.getBuildingId());
        Node node = findNode(req.getNodeId());
        Facility facility = facilityRepository.save(new Facility(req, building, node));
        return new CreateFacilityRes(facility);
    }

    // 편의시설 수정
    @Transactional
    public ModifyFacilityRes modifyFacility(Long facilityId, ModifyFacilityReq req) {
        Building building = findBuilding(req.getBuildingId());
        Node node = findNode(req.getNodeId());
        Facility facility = findFacility(facilityId);
        facility.update(req, building, node);
        return new ModifyFacilityRes();
    }

    // 편의시설 삭제
    @Transactional
    public DeleteFacilityRes deleteFacility(Long facilityId) {
        Facility facility = findFacility(facilityId);
        facilityRepository.delete(facility);

        return new DeleteFacilityRes();
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private Node findNode(Long nodeId) {
        return nodeRepository.findById(nodeId).orElseThrow(() -> new GlobalException(NOT_FOUND_NODE));
    }

    private Facility findFacility(Long facilityId) {
        return facilityRepository.findById(facilityId).orElseThrow(() -> new GlobalException(NOT_FOUND_FACILITY));
    }

}
