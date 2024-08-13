package devkor.com.teamcback.domain.admin.classroom.service;

import devkor.com.teamcback.domain.admin.classroom.dto.request.CreateClassroomReq;
import devkor.com.teamcback.domain.admin.classroom.dto.request.ModifyClassroomReq;
import devkor.com.teamcback.domain.admin.classroom.dto.response.*;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.classroom.repository.ClassroomRepository;
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
public class AdminClassroomService {
    private final BuildingRepository buildingRepository;
    private final ClassroomRepository classroomRepository;
    private final NodeRepository nodeRepository;

    // 건물 id와 층에 해당하는 교실 리스트 조회
    @Transactional(readOnly = true)
    public GetClassroomListRes getClassroomList(Long buildingId, Double floor) {
        Building building = findBuilding(buildingId);
        List<GetClassroomRes> classroomList = classroomRepository.findAllByBuildingAndFloor(building, floor).stream().map(GetClassroomRes::new).collect(Collectors.toList());;
        if(classroomList.isEmpty()) throw new GlobalException(NOT_FOUND_CLASSROOM);

        return new GetClassroomListRes(classroomList);
    }

    // 교실 생성
    @Transactional
    public CreateClassroomRes createClassroom(CreateClassroomReq req) {
        Building building = findBuilding(req.getBuildingId());
        Node node = findNode(req.getNodeId());
        Classroom classroom = classroomRepository.save(new Classroom(req, building, node));

        return new CreateClassroomRes(classroom);
    }

    // 교실 수정
    @Transactional
    public ModifyClassroomRes modifyClassroom(Long classroomId, ModifyClassroomReq req) {
        Building building = findBuilding(req.getBuildingId());
        Node node = findNode(req.getNodeId());
        Classroom classroom = findClassroom(classroomId);
        classroom.update(req, building, node);

        return new ModifyClassroomRes();
    }

    // 교실 삭제
    @Transactional
    public DeleteClassroomRes deleteClassroom(Long classroomId) {
        Classroom classroom = findClassroom(classroomId);
        classroomRepository.delete(classroom);

        return new DeleteClassroomRes();
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private Node findNode(Long nodeId) {
        return nodeRepository.findById(nodeId).orElseThrow(() -> new GlobalException(NOT_FOUND_NODE));
    }

    private Classroom findClassroom(Long classroomId) {
        return classroomRepository.findById(classroomId).orElseThrow(() -> new GlobalException(NOT_FOUND_CLASSROOM));
    }

}
