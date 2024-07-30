package devkor.com.teamcback.domain.admin.route.service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_NODE;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingImage;
import devkor.com.teamcback.domain.building.repository.BuildingImageRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.classroom.repository.ClassroomRepository;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.repository.FacilityRepository;
import devkor.com.teamcback.domain.admin.route.dto.request.CreateNodeReq;
import devkor.com.teamcback.domain.admin.route.dto.request.ModifyNodeReq;
import devkor.com.teamcback.domain.admin.route.dto.response.CreateNodeRes;
import devkor.com.teamcback.domain.admin.route.dto.response.DeleteNodeRes;
import devkor.com.teamcback.domain.admin.route.dto.response.GetNodeDetailRes;
import devkor.com.teamcback.domain.admin.route.dto.response.GetNodeListRes;
import devkor.com.teamcback.domain.admin.route.dto.response.GetNodeRes;
import devkor.com.teamcback.domain.admin.route.dto.response.ModifyNodeRes;
import devkor.com.teamcback.domain.navigate.entity.Node;
import devkor.com.teamcback.domain.navigate.repository.NodeRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRouteService {
    private final NodeRepository nodeRepository;
    private final BuildingRepository buildingRepository;
    private final BuildingImageRepository buildingImageRepository;
    private final ClassroomRepository classroomRepository;
    private final FacilityRepository facilityRepository;

    // 건물 id와 층에 해당하는 건물 사진, 노드 리스트 조회
    @Transactional(readOnly = true)
    public GetNodeListRes getNodeList(Long buildingId, Double floor) {
        Building building = findBuilding(buildingId);
        BuildingImage buildingImage = findBuildingImage(building, floor);
        List<GetNodeRes> nodeList = nodeRepository.findAllByBuildingAndFloor(building, floor).stream().map(node -> new GetNodeRes(node)).toList();

        return new GetNodeListRes(building, floor, buildingImage, nodeList);
    }

    // 노드 단일 조회
    @Transactional(readOnly = true)
    public GetNodeDetailRes getNode(Long nodeId) {
        Node node = findNode(nodeId);

        GetNodeDetailRes res =  new GetNodeDetailRes(node);

        Classroom classroom = classroomRepository.findByNode(node);
        Facility facility = facilityRepository.findByNode(node);

        if(classroom != null) {
            res.setClassroomPlace(classroom);
        }
        else if(facility != null) {
            res.setFacilityPlace(facility);
        }

        return res;
    }

    // 노드 생성
    @Transactional
    public CreateNodeRes createNode(CreateNodeReq req) {
        Building building = findBuilding(req.getBuildingId());
        Node node = nodeRepository.save(new Node(building, req));

        return new CreateNodeRes(node);
    }

    // 노드 수정
    @Transactional
    public ModifyNodeRes modifyNode(Long nodeId, ModifyNodeReq req) {
        Node node = findNode(nodeId);
        Building building = findBuilding(req.getBuildingId());

        node.update(building, req);

        return new ModifyNodeRes();
    }

    // 노드 삭제
    @Transactional
    public DeleteNodeRes deleteNode(Long nodeId) {
        Node node = findNode(nodeId);
        nodeRepository.delete(node);

        return new DeleteNodeRes();
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private BuildingImage findBuildingImage(Building building, Double floor) {
        return buildingImageRepository.findByBuildingAndFloor(building, floor);
    }

    private Node findNode(Long nodeId) {
        return nodeRepository.findById(nodeId).orElseThrow(() -> new GlobalException(NOT_FOUND_NODE));
    }
}