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
import devkor.com.teamcback.domain.common.PlaceType;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import devkor.com.teamcback.domain.facility.repository.FacilityRepository;
import devkor.com.teamcback.domain.search.dto.request.SaveSearchLogReq;
import devkor.com.teamcback.domain.search.dto.response.*;
import devkor.com.teamcback.domain.search.entity.SearchLog;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static devkor.com.teamcback.global.response.ResultCode.*;

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

    // 아이콘으로 표시할 편의시설 종류
    private final List<FacilityType> iconTypes = Arrays.asList(FacilityType.VENDING_MACHINE, FacilityType.PRINTER, FacilityType.LOUNGE, FacilityType.READING_ROOM, FacilityType.STUDY_ROOM, FacilityType.CAFE, FacilityType.CONVENIENCE_STORE, FacilityType.CAFETERIA, FacilityType.SLEEPING_ROOM, FacilityType.SHOWER_ROOM, FacilityType.BANK, FacilityType.GYM);

    /**
     * 검색어 자동 완성
     */
    @Transactional(readOnly = true)
    public GlobalSearchListRes globalSearch(Long buildingId, String word) {
        List<GlobalSearchRes> list = new ArrayList<>();

        List<Classroom> classrooms = getClassrooms(word);
        List<Facility> facilities = getFacilities(word);

        // 먼저 검색한 건물이 있을 때
        if(buildingId != null) {
            Building building = findBuilding(buildingId);
            for(Classroom classroom : classrooms) {
                if(classroom.getBuilding().equals(building)) list.add(new GlobalSearchRes(classroom, PlaceType.CLASSROOM));
            }
            for(Facility facility : facilities) {
                if(facility.getBuilding().equals(building)) list.add(new GlobalSearchRes(facility, PlaceType.FACILITY));
            }
        }

        else {
            List<Building> buildings = getBuildings(word);

            for(Building building : buildings) {
                list.add(new GlobalSearchRes(building, PlaceType.BUILDING));
            }
            for(Classroom classroom : classrooms) {
                list.add(new GlobalSearchRes(classroom, PlaceType.CLASSROOM));
            }
            for(Facility facility : facilities) {
                list.add(new GlobalSearchRes(facility, PlaceType.FACILITY));
            }
        }

        return new GlobalSearchListRes(list);
    }

    /**
     * 모든 건물 검색
     */
    @Transactional(readOnly = true)
    public SearchBuildingListRes searchAllBuildings() {
        List<Building> buildingList = buildingRepository.findAll();

        return new SearchBuildingListRes(
            buildingList.stream()
            .filter(building -> building.getId() != 0)
            .map(building -> {
                List<FacilityType> containFacilityTypes = getFacilitiesByBuildingAndTypes(building, iconTypes).stream()
                    .map(Facility::getType)
                    .distinct()
                    .toList();
                return new SearchBuildingRes(building, containFacilityTypes);
            })
            .collect(Collectors.toList()));
    }

    /**
     * 건물 내 특정 종류의 편의시설 검색
     */
    @Transactional(readOnly = true)
    public SearchFacilityListRes searchBuildingFacilityByType(Long buildingId, FacilityType facilityType) {
        Building building = findBuilding(buildingId);
        SearchFacilityListRes res = new SearchFacilityListRes(building, facilityType);

        List<Facility> facilities = facilityRepository.findAllByBuildingAndType(building, facilityType);
        Map<Double, List<SearchFacilityRes>> map = new HashMap<>();
        for(Facility facility : facilities) {
            if(!map.containsKey(facility.getFloor())) map.put(facility.getFloor(), new ArrayList<>());
            map.get(facility.getFloor()).add(new SearchFacilityRes(facility));
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

         List<SearchRoomDetailRes> roomDetailRes = new ArrayList<>(
             classroomList.stream().map(SearchRoomDetailRes::new).toList());
         roomDetailRes.addAll(facilityList.stream().map(SearchRoomDetailRes::new).toList());

         return new SearchRoomRes(roomDetailRes);
    }

    /**
     * 편의시설이 있는 건물
     */
    public SearchBuildingListRes searchBuildingWithFacilityType(FacilityType facilityType) {
        List<Facility> facilityList = facilityRepository.findAllByType(facilityType);
        List<Building> buildingList = facilityList.stream()
            .map(Facility::getBuilding)
            .distinct()
            .toList();

        return new SearchBuildingListRes(
            buildingList.stream()
                .filter(building -> building.getId() != 0)
                .map(building -> {
                    List<FacilityType> containFacilityTypes = getFacilitiesByBuildingAndTypes(building, iconTypes).stream()
                        .map(Facility::getType)
                        .distinct()
                        .toList();
                    return new SearchBuildingRes(building, containFacilityTypes);
                })
                .toList());
    }

    /**
     * Mask Index 대응 교실 조회
     */
    @Transactional(readOnly = true)
    public SearchPlaceByMaskIndexRes searchPlaceByMaskIndex(Long buildingId, int floor, Integer maskIndex, PlaceType type) {
        Building building = findBuilding(buildingId);
        SearchPlaceByMaskIndexRes res = new SearchPlaceByMaskIndexRes();

        switch (type) {
            case CLASSROOM -> {
                res = new SearchPlaceByMaskIndexRes(classroomRepository.findByBuildingAndFloorAndMaskIndex(building, floor, maskIndex));
            }
            case FACILITY -> {
                res =  new SearchPlaceByMaskIndexRes(facilityRepository.findByBuildingAndFloorAndMaskIndex(building, floor, maskIndex));
            }
        }
        return res;
    }

    /**
     * 교실 대응 Mask Index 조회
     */
    @Transactional(readOnly = true)
    public SearchMaskIndexByPlaceRes searchMaskIndexByPlace(Long placeId, PlaceType type) {
        SearchMaskIndexByPlaceRes res = new SearchMaskIndexByPlaceRes();
        switch (type) {
            case CLASSROOM -> {
                res = new SearchMaskIndexByPlaceRes(findClassroom(placeId));
            }
            case FACILITY -> {
                res =  new SearchMaskIndexByPlaceRes(findFacility(placeId));
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
        List<Facility> mainFacilities = getFacilitiesByBuildingAndTypes(building, types);
        List<SearchMainFacilityRes> res = new ArrayList<>();

        for (Facility facility : mainFacilities) {
            res.add(new SearchMainFacilityRes(facility));
        }

        List<FacilityType> containFacilityTypes = getFacilitiesByBuildingAndTypes(building, iconTypes).stream()
            .map(Facility::getType)
            .distinct()
            .toList();

        //즐겨찾기 여부 확인 (로그인 X -> false)
        boolean bookmarked = false;
        if(userId != null) {
            User user = findUser(userId);
            // 해당 유저의 북마크에 빌딩 있는지 확인 (유저의 카테고리 리스트 가져와서, 해당 안에 존재하는지 확인)
            List<Category> categories = categoryRepository.findAllByUser(user);
            if(bookmarkRepository.existsByPlaceTypeAndPlaceIdAndCategoryIn(PlaceType.BUILDING, buildingId, categories)) {
                bookmarked = true;
            }
        }

        // TODO: 커뮤니티 구상 완료되면 커뮤니티 정보 넣기

        return new SearchBuildingDetailRes(res, containFacilityTypes, building, bookmarked);
    }

    /**
     * 장소(교실, 편의시설) 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public SearchPlaceDetailRes searchPlaceDetail(Long userId, Long placeId, PlaceType placeType) {
        SearchPlaceDetailRes res = new SearchPlaceDetailRes();
        List<Category> categories = new ArrayList<>();
        boolean bookmarked = false;

        //즐겨찾기 여부 확인용 정보 가져오기
        if(userId != null) {
            User user = findUser(userId);
            categories = categoryRepository.findAllByUser(user);
        }

        switch (placeType) {
            case CLASSROOM -> {
                Classroom classroom = findClassroom(placeId);
                if(bookmarkRepository.existsByPlaceTypeAndPlaceIdAndCategoryIn(PlaceType.CLASSROOM, classroom.getId(), categories)) {
                    bookmarked = true;
                }
                res = new SearchPlaceDetailRes(classroom, bookmarked);
            }
            case FACILITY -> {
                Facility facility = findFacility(placeId);
                if(bookmarkRepository.existsByPlaceTypeAndPlaceIdAndCategoryIn(PlaceType.FACILITY, facility.getId(), categories)) {
                    bookmarked = true;
                }
                res =  new SearchPlaceDetailRes(facility, bookmarked);
            }
        }

        return res;
    }

    private List<Facility> getFacilitiesByBuildingAndTypes(Building building, List<FacilityType> types) {
        return facilityRepository.findAllByBuildingAndTypeInOrderByFloor(building, types);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }

    /**
     * 검색 기록 조회
     */
    public List<SearchLogRes> getSearchLog(Long userId) {
        List<SearchLog> searchLogs = searchLogRedis.opsForList().range(String.valueOf(userId), 0, 10);
        List<SearchLogRes> resList = new ArrayList<>();
        if(searchLogs != null) {
            for (SearchLog searchLog : searchLogs) {
                resList.add(new SearchLogRes(searchLog));
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
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }
    private Classroom findClassroom(Long classroomId) {
        return classroomRepository.findById(classroomId).orElseThrow(() -> new GlobalException(NOT_FOUND_CLASSROOM));
    }
    private Facility findFacility(Long facilityId) {
        return facilityRepository.findById(facilityId).orElseThrow(() -> new GlobalException(NOT_FOUND_FACILITY));
    }
}
