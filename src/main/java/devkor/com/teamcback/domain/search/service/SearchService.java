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
import devkor.com.teamcback.domain.search.dto.response.GetBuildingDetailRes;
import devkor.com.teamcback.domain.search.dto.response.GetFacilityRes;
import devkor.com.teamcback.domain.search.dto.response.GetRoomDetailRes;
import devkor.com.teamcback.domain.search.dto.response.GetSearchLogRes;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchRes;
import devkor.com.teamcback.domain.search.dto.response.SearchBuildingRes;
import devkor.com.teamcback.domain.search.dto.response.SearchFacilityRes;
import devkor.com.teamcback.domain.search.dto.response.SearchFacilityTypeRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceRes;
import devkor.com.teamcback.domain.search.dto.response.SearchRoomRes;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import devkor.com.teamcback.domain.search.entity.SearchLog;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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

    /**
     * 검색어 자동 완성
     */
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

    /**
     * 건물 또는 강의실 검색
     */
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

    /**
     * 모든 건물 검색
     */
    @Transactional(readOnly = true)
    public SearchBuildingRes searchAllBuildings() {
        return new SearchBuildingRes(buildingRepository.findAll().stream().map(GetBuildingDetailRes::new).toList());
    }

    /**
     * 건물 내 특정 종류의 편의시설 검색
     */
    @Transactional(readOnly = true)
    public SearchFacilityRes searchBuildingFacilityByType(Long buildingId, FacilityType facilityType) {
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
        List<FacilityType> result = new ArrayList<>();

        for (FacilityType facilityType : FacilityType.values()) {
            if (facilityType.getName().contains(word)) {
                result.add(facilityType);
            }
        }

        return result;
    }

    /**
     * 건물에 있는 편의시설 종류 검색
     */
    @Transactional(readOnly = true)
    public SearchFacilityTypeRes searchFacilityTypeByBuilding(Long buildingId) {
        Building building = findBuilding(buildingId);
        List<Facility> facilities = facilityRepository.findAllByBuilding(building);
        List<FacilityType> facilityTypeList = facilities.stream()
            .map(Facility::getType)
            .distinct()
            .toList();

        return new SearchFacilityTypeRes(facilityTypeList);
    }

    /**
     * 건물 특정 층에 있는 강의실과 편의시설 검색
     */
    @Transactional(readOnly = true)
    public SearchRoomRes searchRoomByBuildingFloor(Long buildingId, int floor) {
         Building building = findBuilding(buildingId);
         List<Classroom> classroomList = classroomRepository.findAllByBuildingAndFloor(building, floor);
         List<Facility> facilityList = facilityRepository.findAllByBuildingAndFloor(building, floor);

         List<GetRoomDetailRes> roomDetailRes = new ArrayList<>(
             classroomList.stream().map(GetRoomDetailRes::new).toList());
         roomDetailRes.addAll(facilityList.stream().map(GetRoomDetailRes::new).toList());

         return new SearchRoomRes(roomDetailRes);
    }

    /**
     * 편의시설이 있는 건물
     */
    public SearchBuildingRes searchBuildingWithFacilityType(FacilityType facilityType) {
        List<Facility> facilityList = facilityRepository.findAllByType(facilityType);
        List<Building> buildingList = facilityList.stream()
            .map(Facility::getBuilding)
            .distinct()
            .toList();

        List<GetBuildingDetailRes> buildingDetailRes = buildingList.stream().map(GetBuildingDetailRes::new).toList();
        return new SearchBuildingRes(buildingDetailRes);
    }

    /**
     * 검색 기록 조회
     */
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

    /**
     * 검색 기록 저장
     */
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
