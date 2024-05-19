package devkor.com.teamcback.domain.search.service;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import devkor.com.teamcback.domain.building.repository.BuildingNicknameRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.classroom.entity.ClassroomNickname;
import devkor.com.teamcback.domain.classroom.repository.ClassroomNicknameRepository;
import devkor.com.teamcback.domain.classroom.repository.ClassroomRepository;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import devkor.com.teamcback.domain.facility.repository.FacilityRepository;
import devkor.com.teamcback.domain.search.dto.request.SaveSearchLogReq;
import devkor.com.teamcback.domain.search.dto.request.SearchFacilityReq;
import devkor.com.teamcback.domain.search.dto.request.SearchPlaceReq;
import devkor.com.teamcback.domain.search.dto.response.GetFacilityRes;
import devkor.com.teamcback.domain.search.dto.response.GetSearchLogRes;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchRes;
import devkor.com.teamcback.domain.search.dto.response.SearchFacilityRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceRes;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import devkor.com.teamcback.domain.search.entity.SearchLog;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties.Build;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final BuildingRepository buildingRepository;
    private final BuildingNicknameRepository buildingNicknameRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomNicknameRepository classroomNicknameRepository;
    private final FacilityRepository facilityRepository;
    private final RedisTemplate<String, SearchLog> searchLogRedis;

    @Transactional(readOnly = true)
    public List<GlobalSearchRes> globalSearch(Long buildingId, String word) {
        List<GlobalSearchRes> resList = new ArrayList<>();

        List<Classroom> classrooms = getClassrooms(word);
        List<FacilityType> facilities = getFacilities(word);

        // 먼저 검색한 건물이 있을 때
        if(buildingId != null) {
            Building building = findBuilding(buildingId);
            for(Classroom classroom : classrooms) {
                if(classroom.getBuilding().equals(building)) resList.add(new GlobalSearchRes(classroom, PlaceType.CLASSROOM));
            }
            for(FacilityType facilityType : facilities) {
                // 편의시설의 종류가 해당 건물에 있는지 확인
                if(facilityRepository.existsByBuildingAndType(building, facilityType)) resList.add(new GlobalSearchRes(facilityType, PlaceType.FACILITY));
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
            for(FacilityType facilityType : facilities) {
                resList.add(new GlobalSearchRes(facilityType, PlaceType.FACILITY));
            }
        }

        return resList;
    }

    @Transactional(readOnly = true)
    public SearchPlaceRes searchPlace(PlaceType type, Long id) {
        SearchPlaceRes res = new SearchPlaceRes();
        switch (type) {
            case BUILDING -> {
                res = new SearchPlaceRes(buildingRepository.findBuildingById(id));
            }
            case CLASSROOM -> {
                res =  new SearchPlaceRes(classroomRepository.findClassroomById(id));
            }
        }

        return res;
    }

    @Transactional(readOnly = true)
    public SearchFacilityRes searchFacility(Long buildingId, FacilityType facilityType) {
        Building building = findBuilding(buildingId);
        SearchFacilityRes res = new SearchFacilityRes(building, facilityType);

        List<Facility> facilities = facilityRepository.findAllByBuildingAndType(building, facilityType);
        Map<Integer, List<GetFacilityRes>> map = new HashMap<>();
        for(Facility facility : facilities) {
            if(!map.containsKey(facility.getFloor())) map.put(facility.getFloor(), new ArrayList<>());
            map.get(facility.getFloor()).add(new GetFacilityRes(facility));
        }

        res.setFacilities(map);

        return res;
    }

    private List<Building> getBuildings(String word) {
        // 건물 조회
        List<BuildingNickname> buildingNicknames = buildingNicknameRepository.findByNicknameContaining(word);

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

    // 검색어를 포함하는 편의시설의 종류 리스트를 반환
    private List<FacilityType> getFacilities(String word) {
//        // 편의시설 조회
//        List<Facility> facilities = facilityRepository.findByNameContaining(word);
//
//        // 중복을 제거하여 List에 저장 (building & name(종류)이 모두 겹치는 경우를 제거)
//        Set<String> seen = new HashSet<>();
//        return facilities.stream()
//            .filter(facility -> seen.add(facility.getName() + "-" + facility.getBuilding().getId()))
//            .toList();
        List<FacilityType> result = new ArrayList<>();

        for (FacilityType facilityType : FacilityType.values()) {
            if (facilityType.getName().contains(word)) {
                result.add(facilityType);
            }
        }

        return result;
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

        // TODO: 중복되는 검색 기록 제거
        if(size != null && size.equals(10L)) {
            searchLogRedis.opsForList().rightPop(key);
        }

        searchLogRedis.opsForList().leftPush(key, searchLog);
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow();
    }
}
