package devkor.com.teamcback.domain.search.service;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import devkor.com.teamcback.domain.building.repository.BuildingNicknameRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.classroom.entity.ClassroomNickname;
import devkor.com.teamcback.domain.classroom.repository.ClassroomNicknameRepository;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.repository.FacilityRepository;
import devkor.com.teamcback.domain.search.dto.request.SaveSearchLogReq;
import devkor.com.teamcback.domain.search.dto.response.GetSearchLogRes;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchRes;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import devkor.com.teamcback.domain.search.entity.SearchLog;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final BuildingRepository buildingRepository;
    private final BuildingNicknameRepository buildingNicknameRepository;
    private final ClassroomNicknameRepository classroomNicknameRepository;
    private final UserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final RedisTemplate<String, SearchLog> searchLogRedis;

    @Transactional(readOnly = true)
    public List<GlobalSearchRes> globalSearch(Long buildingId, String word) {
        List<GlobalSearchRes> resList = new ArrayList<>();

        List<Classroom> classrooms = getClassrooms(word);

        List<Facility> facilities = getFacilities(word);

        // 먼저 검색한 건물이 있을 때
        if(buildingId != null) {
            Building building = findBuilding(buildingId);
            for(Classroom classroom : classrooms) {
                if(classroom.getBuilding().equals(building)) resList.add(new GlobalSearchRes(classroom, PlaceType.CLASSROOM));
            }
            for(Facility facility : facilities) {
                if(facility.getBuilding().equals(building)) resList.add(new GlobalSearchRes(facility, PlaceType.FACILITY));
            }
        }

        else {
            List<Building> buildings = getBuildings(word);

            for(Building building : buildings) {
                resList.add(new GlobalSearchRes(building, PlaceType.BUILDING));
            }
            for(Classroom classroom : classrooms) {
                resList.add(new GlobalSearchRes(classroom, PlaceType.CLASSROOM));
            }
            for(Facility facility : facilities) {
                resList.add(new GlobalSearchRes(facility, PlaceType.FACILITY));
            }
        }

        return resList;
    }

    private List<Building> getBuildings(String word) {
        // 건물 조회
        List<BuildingNickname> buildingNicknames = buildingNicknameRepository.findByNicknameContaining(
            word);

        // 중복을 제거하여 List에 저장
        return buildingNicknames.stream()
            .map(BuildingNickname::getBuilding)
            .distinct()
            .toList();
    }

    private List<Classroom> getClassrooms(String word) {
        // 강의실 조회
        List<ClassroomNickname> classroomNicknames = classroomNicknameRepository.findByNicknameContaining(word);

        // 중복을 제거하여 List에 저장
        return classroomNicknames.stream()
            .map(ClassroomNickname::getClassroom)
            .distinct()
            .toList();
    }

    private List<Facility> getFacilities(String word) {
        // 편의시설 조회
        return facilityRepository.findByNameContaining(word);
    }

    public List<GetSearchLogRes> getSearchLog(Long userId) {
        List<SearchLog> searchLogs = searchLogRedis.opsForList().range(String.valueOf(userId), 0, 10);
        List<GetSearchLogRes> resList = new ArrayList<>();
        if(searchLogs != null) {
            for (SearchLog searchLog : searchLogs) {
                resList.add(new GetSearchLogRes(searchLog));
            }
        }
        return resList;
    }

    public void saveSearchLog(Long userId, SaveSearchLogReq req) {
        String searchedAt = LocalDate.now().toString();
        SearchLog searchLog = new SearchLog(req.getId(), req.getName(), req.getType(), searchedAt);
        String key = String.valueOf(userId);

        Long size = searchLogRedis.opsForList().size(key);

        if(size != null && size.equals(10L)) {
            searchLogRedis.opsForList().rightPop(key);
        }

        searchLogRedis.opsForList().leftPush(key, searchLog);
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow();
    }

}
