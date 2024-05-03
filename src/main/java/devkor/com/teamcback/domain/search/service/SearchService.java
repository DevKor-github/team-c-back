package devkor.com.teamcback.domain.search.service;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import devkor.com.teamcback.domain.building.repository.BuildingNicknameRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.classroom.entity.ClassroomNickname;
import devkor.com.teamcback.domain.classroom.repository.ClassroomNicknameRepository;
import devkor.com.teamcback.domain.search.dto.response.AutoCompleteRes;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final BuildingRepository buildingRepository;
    private final BuildingNicknameRepository buildingNicknameRepository;
    private final ClassroomNicknameRepository classroomNicknameRepository;

    @Transactional(readOnly = true)
    public List<AutoCompleteRes> autoComplete(Long buildingId, String word) {
        List<AutoCompleteRes> resList = new ArrayList<>();

        // TODO: 편의시설 조회 추가

        // 강의실 조회
        List<ClassroomNickname> classroomNicknames = classroomNicknameRepository.findByNicknameContaining(word);

        // 중복을 제거하여 List에 저장
        List<Classroom> classrooms = classroomNicknames.stream()
            .map(ClassroomNickname::getClassroom)
            .distinct()
            .toList();

        // 먼저 검색한 건물이 있을 때
        if(buildingId != null) {
            Building building = findBuilding(buildingId);
            for(Classroom classroom : classrooms) {
                if(classroom.getBuilding().equals(building)) resList.add(new AutoCompleteRes(classroom, PlaceType.CLASSROOM));
            }
        }

        else {
            // 건물 조회
            List<BuildingNickname> buildingNicknames = buildingNicknameRepository.findByNicknameContaining(word);

            // 중복을 제거하여 List에 저장
            List<Building> buildings = buildingNicknames.stream()
                .map(BuildingNickname::getBuilding)
                .distinct()
                .toList();

            for(Building building : buildings) {
                resList.add(new AutoCompleteRes(building, PlaceType.BUILDING));
            }
            for(Classroom classroom : classrooms) {
                resList.add(new AutoCompleteRes(classroom, PlaceType.CLASSROOM));
            }
        }

        return resList;
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow();
    }
}
