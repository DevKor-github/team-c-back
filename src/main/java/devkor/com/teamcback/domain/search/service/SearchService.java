package devkor.com.teamcback.domain.search.service;

import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.repository.BookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
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
import devkor.com.teamcback.domain.search.dto.response.*;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import devkor.com.teamcback.domain.search.entity.SearchLog;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final BuildingRepository buildingRepository;
    private final BuildingNicknameRepository buildingNicknameRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomNicknameRepository classroomNicknameRepository;
    private final FacilityRepository facilityRepository;
    private final RedisTemplate<String, SearchLog> searchLogRedis;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookmarkRepository bookmarkRepository;

    /**
     * 검색어 자동 완성
     */
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
        List<Building> buildingList = buildingRepository.findAll();
        return new SearchBuildingRes(buildingList.stream().filter(building -> building.getId() != 0).map(GetBuildingDetailRes::new).toList());
    }

    /**
     * 건물 내 특정 종류의 편의시설 검색
     */
    @Transactional(readOnly = true)
    public SearchFacilityRes searchBuildingFacilityByType(Long buildingId, FacilityType facilityType) {
        Building building = findBuilding(buildingId);
        SearchFacilityRes res = new SearchFacilityRes(building, facilityType);

        List<Facility> facilities = facilityRepository.findAllByBuildingAndType(building, facilityType);
        Map<Double, List<GetFacilityRes>> map = new HashMap<>();
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

    // 검색어를 포함하는 편의시설의 리스트를 반환 (편의시설의 name을 반환)
    private List<Facility> getFacilities(String word) {
        List<Facility> facilities = facilityRepository.findByNameContaining(word);

        return facilities.stream()
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(Facility::getName, f -> f, (existing, replacement) -> existing),
                map -> new ArrayList<>(map.values())
            ));
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
     * Mask Index 대응 교실 조회
     */
    @Transactional(readOnly = true)
    public searchPlaceByMaskIndexRes searchPlaceByMaskIndex(Long buildingId, int floor, PlaceType type, Integer maskIndex) {
        Building building = findBuilding(buildingId);
        searchPlaceByMaskIndexRes res = new searchPlaceByMaskIndexRes();

        switch (type) {
            case CLASSROOM -> {
                res = new searchPlaceByMaskIndexRes(classroomRepository.findByBuildingAndFloorAndMaskIndex(building, floor, maskIndex));
            }
            case FACILITY -> {
                res =  new searchPlaceByMaskIndexRes(facilityRepository.findByBuildingAndFloorAndMaskIndex(building, floor, maskIndex));
            }
        }
        return res;
    }

    /**
     * 건물 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public SearchBuildingDetailRes searchBuildingDetail(Long userId, Long buildingId) {
        Building building = findBuilding(buildingId);

        //(임의) 가져올 대표 시설 정보 List (라운지, 카페, 편의점, 식당, 헬스장, 열람실, 스터디룸, 수면실, 샤워실)
        //자세한 회의 후 List 수정하기
        List<FacilityType> types = Arrays.asList(FacilityType.LOUNGE, FacilityType.CAFE, FacilityType.CONVENIENCE_STORE, FacilityType.CAFETERIA, FacilityType.READING_ROOM, FacilityType.STUDY_ROOM, FacilityType.GYM, FacilityType.SLEEPING_ROOM, FacilityType.SHOWER_ROOM);
        List<Facility> mainFacilities = facilityRepository.findAllByBuildingAndTypeInOrderByFloor(building, types);
        List<GetMainFacilityRes> res = new ArrayList<>();

        for (Facility facility : mainFacilities) {
            res.add(new GetMainFacilityRes(facility));
        }

        // 건물 내 편의시설 종류 정보
        SearchFacilityTypeRes containFacilities = searchFacilityTypeByBuilding(buildingId);

        //즐겨찾기 여부 확인 (로그인 X -> false)
        boolean favorite = false;
        if(userId != null) {
            User user = findUser(userId);
            // 해당 유저의 북마크에 빌딩 있는지 확인 (유저의 카테고리 리스트 가져와서, 해당 안에 존재하는지 확인)
            List<Category> categories = categoryRepository.findAllByUser(user);
            //PlaceType 엔티티가 2개라서 이렇게밖에 안되는데 더 나은 코드 아시는 분 계시면 리뷰 남겨주시면 감사하겠습니다!ㅠㅠ
            if(bookmarkRepository.existsByPlaceTypeAndPlaceIdAndCategoryIn(devkor.com.teamcback.domain.bookmark.entity.PlaceType.BUILDING, buildingId, categories)) {
                favorite = true;
            }
        }

        //운영시간 정보, 운영여부 t/f 나중에 넣기 (운영시간 완성되면)
        //커뮤니티 구상 완료되면 커뮤니티 정보도..?

        return new SearchBuildingDetailRes(res, containFacilities.getTypeList(), building, favorite);
    }

    private User findUser(Long userId) {
        Optional<User> user =  userRepository.findById(userId);
        return user.orElse(null);
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
