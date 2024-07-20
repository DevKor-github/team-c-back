package devkor.com.teamcback.domain.navigate.service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_NODE;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingImage;
import devkor.com.teamcback.domain.building.repository.BuildingImageRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.classroom.repository.ClassroomRepository;
import devkor.com.teamcback.domain.facility.repository.FacilityRepository;
import devkor.com.teamcback.domain.navigate.dto.request.CreateNodeReq;
import devkor.com.teamcback.domain.navigate.dto.request.ModifyNodeReq;
import devkor.com.teamcback.domain.navigate.dto.response.CreateNodeRes;
import devkor.com.teamcback.domain.navigate.dto.response.GetNodeListRes;
import devkor.com.teamcback.domain.navigate.dto.response.GetNodeRes;
import devkor.com.teamcback.domain.navigate.dto.response.ModifyNodeRes;
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

    // 노드 생성
    @Transactional
    public CreateNodeRes createNode(CreateNodeReq req) {
//        if(req.getNodeId() != null) checkNodeDuplication(req.getNodeId()); // 요청한 노드 ID에 해당하는 노드가 있는지 확인
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

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private BuildingImage findBuildingImage(Building building, Double floor) {
        return buildingImageRepository.findByBuildingAndFloor(building, floor);
    }

    private Node findNode(Long nodeId) {
        return nodeRepository.findById(nodeId).orElseThrow(() -> new GlobalException(NOT_FOUND_NODE));
    }

//    private void checkNodeDuplication(Long nodeId) {
//        Optional<Node> node = nodeRepository.findById(nodeId);
//        if(node.isPresent()) {
//            throw new GlobalException(DUPLICATED_NODE_ID);
//        }
//    }
}
