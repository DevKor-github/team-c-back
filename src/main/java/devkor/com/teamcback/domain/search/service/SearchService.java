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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Slf4j
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
    private final List<FacilityType> iconTypes = Arrays.asList(FacilityType.VENDING_MACHINE, FacilityType.PRINTER, FacilityType.LOUNGE,
        FacilityType.READING_ROOM, FacilityType.STUDY_ROOM, FacilityType.CAFE, FacilityType.CONVENIENCE_STORE, FacilityType.CAFETERIA,
        FacilityType.SLEEPING_ROOM, FacilityType.SHOWER_ROOM, FacilityType.BANK, FacilityType.GYM);

    /**
     * 통합 검색
     */
    @Transactional(readOnly = true)
    public GlobalSearchListRes globalSearch(String word) {
        List<GlobalSearchRes> list = new ArrayList<>();
        Map<GlobalSearchRes, Integer> scores = new HashMap<>();

        List<Classroom> classrooms;
        List<Facility> facilities;
        List<Building> buildings;

        // 건물이 입력됨
        if(word.contains(" ")) {
            String firstBuildingWord = word.split(" ")[0];
            String lastBuildingWord = word.split(" ")[word.split(" ").length - 1];
            String firstPlaceWord = word.substring(firstBuildingWord.length()).trim();
            String lastPlaceWord = word.substring(0, word.length() - lastBuildingWord.length()).trim();

            buildings = getBuildings(firstBuildingWord);
            if(!buildings.isEmpty()) {
                scores.putAll(getScores(word, buildings, firstPlaceWord, firstBuildingWord));
            }

            buildings = getBuildings(lastBuildingWord);
            if(!buildings.isEmpty()) {
                scores.putAll(getScores(word, buildings, lastPlaceWord, lastBuildingWord));
            }
            return new GlobalSearchListRes(orderSequence(scores));
        }

        buildings = getBuildings(word);
        facilities = getFacilities(word, null);
        classrooms = getClassrooms(word, null);

        for(Building building : buildings) {
            list.add(new GlobalSearchRes(building, PlaceType.BUILDING));
        }
        for(Classroom classroom : classrooms) {
            list.add(new GlobalSearchRes(classroom, PlaceType.CLASSROOM));
        }
        for(Facility facility : facilities) {
            list.add(new GlobalSearchRes(facility, PlaceType.FACILITY, false));
        }

        return new GlobalSearchListRes(orderSequence(calculateScore(list, word, null)));
    }

    /**
     * 모든 or 편의시설에 해당하는 건물 검색
     */
    @Transactional(readOnly = true)
    public SearchBuildingListRes searchBuildings(FacilityType type) {
        List<Building> buildingList;
        if(type == null) {
            buildingList = buildingRepository.findAll();

            buildingList = buildingList.stream()
                .filter(building -> building.getId() != 0).toList();
        }

        else {
            List<Facility> facilityList = getFacilitiesByType(type);
            buildingList = facilityList.stream()
                .map(Facility::getBuilding)
                .distinct()
                .toList();
        }

        return new SearchBuildingListRes(
            buildingList.stream()
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
     * 건물 내 특정 종류의 편의시설 검색
     */
    @Transactional(readOnly = true)
    public SearchBuildingFacilityListRes searchBuildingFacilityByType(Long buildingId, FacilityType facilityType) {
        Building building = findBuilding(buildingId);
        SearchBuildingFacilityListRes res = new SearchBuildingFacilityListRes(building, facilityType);

        List<Facility> facilities = getFacilitiesByBuildingAndType(building, facilityType);

        Map<Double, List<SearchFacilityRes>> map = new HashMap<>();
        for(Facility facility : facilities) {
            if(!map.containsKey(facility.getFloor())) map.put(facility.getFloor(), new ArrayList<>());
            map.get(facility.getFloor()).add(new SearchFacilityRes(facility));
        }

        res.setFacilities(map);

        return res;
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
     * 편의시설 목록 조회
     */
    @Transactional(readOnly = true)
    public SearchFacilityListRes searchFacilitiesWithType(FacilityType facilityType) {
        // TODO: type이 카페/식당/편의점이 아닌 경우에 예외를 발생할 것인지?
        List<SearchFacilityRes> facilityList = facilityRepository.findAllByType(facilityType).stream().map(SearchFacilityRes::new).toList();

        return new SearchFacilityListRes(facilityList);
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

    private List<Building> getBuildings(String word) {
        // 건물 조회
        List<BuildingNickname> buildingNicknames = buildingNicknameRepository.findByNicknameContaining(word);

        // 중복을 제거하여 List에 저장
        return buildingNicknames.stream()
            .map(BuildingNickname::getBuilding)
            .distinct()
            .toList();
    }

    private Map<GlobalSearchRes, Integer> getScores(String word, List<Building> buildings, String placeWord, String buildingWord) {
        List<GlobalSearchRes> list = new ArrayList<>();
        List<Classroom> classrooms;
        List<Facility> facilities;

        for (Building building : buildings) {
            list.add(new GlobalSearchRes(building, PlaceType.BUILDING));
            facilities = getFacilities(placeWord, building);
            classrooms = getClassrooms(placeWord, building);

            for (Classroom classroom : classrooms) {
                list.add(new GlobalSearchRes(classroom, PlaceType.CLASSROOM));
            }
            for (Facility facility : facilities) {
                list.add(new GlobalSearchRes(facility, PlaceType.FACILITY, true));
            }
        }
        return calculateScore(list, word, buildingWord);
    }

    private Map<GlobalSearchRes, Integer> calculateScore(List<GlobalSearchRes> list, String keyword, String buildingKeyword) {
        //TODO : 로그인 반영 후, 즐겨찾기 여부 확인해서 맨 위로 올리기 -> baseScore = 5000
        Map<GlobalSearchRes, Integer> scores = new HashMap<>();
        // 강의실, 특수명 편의시설은 baseScore = 0
        int baseScore = 0;
        int indexScore = 0;

        for (GlobalSearchRes res : list) {
            if (res.getPlaceType() == PlaceType.BUILDING) {
                baseScore = buildingKeyword == null ? 1000 : -500;
                indexScore = buildingKeyword != null ? calculateScoreByIndex(res.getName(), buildingKeyword) : calculateScoreByIndex(res.getName(), keyword);
            }
            if (res.getPlaceType() == PlaceType.FACILITY) {
                if(res.getName().contains(" ")) {
                    baseScore = checkFacilityType(res.getName().split(" ", 2)[1]) ? 500 : 0;
                } else {
                    baseScore = 500;
                }
                indexScore = calculateScoreByIndex(res.getName(), keyword);
            }
            if (res.getPlaceType() == PlaceType.CLASSROOM) {
                baseScore = 0;
                indexScore = calculateScoreByIndex(res.getName(), keyword);
            }
            scores.put(res, baseScore + indexScore);
        }
        return scores;
    }

    private static List<GlobalSearchRes> orderSequence(Map<GlobalSearchRes, Integer> scores) {
        // 점수를 기준으로 그룹화
        Map<Integer, List<GlobalSearchRes>> groupedByScore = scores.entrySet().stream()
            .collect(Collectors.groupingBy(
                Map.Entry::getValue,
                Collectors.mapping(Map.Entry::getKey, Collectors.toList())
            ));

        // 각 그룹 내의 리스트를 이름 기준으로 정렬
        Map<Integer, List<GlobalSearchRes>> sortedGroupedByScore = groupedByScore.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .sorted(Comparator.comparing(GlobalSearchRes::getName))
                    .toList(),
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));

        // 정렬된 그룹을 점수 순으로 재결합
        return sortedGroupedByScore.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .flatMap(entry -> entry.getValue().stream())
            .toList();
    }

    private static boolean checkFacilityType(String name) {
        return Arrays.stream(FacilityType.values())
            .anyMatch(facilityType -> facilityType.getName().equals(name));
    }

    private int calculateScoreByIndex(String name, String keyword) {
        // 괄호() > 대학 > 관 (앞의 것이 존재한다면 해당 것을 기준으로 삼음)
        String[] criteria = {"\\(", "대학", "관"};
        int indexScore = 0;

        for (String c : criteria) {
            if(name.contains(c.replace("\\", ""))) {
                String[] temp = name.split(c, 2);
                if(!temp[1].isEmpty()) {
                    name = temp[1];
                    break;
                }
            }
        }

        int index = name.indexOf(keyword.charAt(0));
        if (index != -1) indexScore += (100 - index);

        return indexScore;
    }

    private List<Classroom> getClassrooms(String word, Building building) {
        List<Classroom> classrooms = new ArrayList<>();
        if(building != null) {
            classrooms = classroomRepository.findAllByBuilding(building);
        }

        //응답 개수 제한
        Pageable limit = PageRequest.of(0, 30, Sort.by("id").descending());

        // 강의실 조회
        List<ClassroomNickname> classroomNicknames = new ArrayList<>();
        if(building != null && !classrooms.isEmpty()) {
            classroomNicknames = classroomNicknameRepository.findByNicknameContainingAndClassroomInOrderByNickname(word, classrooms, limit);
        }
        else if(building == null) {
            classroomNicknames = classroomNicknameRepository.findByNicknameContainingOrderByNickname(word, limit);
        }


        // 중복을 제거하여 List에 저장
        return classroomNicknames.stream()
            .map(ClassroomNickname::getClassroom)
            .distinct()
            .toList();
    }

    // 검색어를 포함하는 편의시설의 리스트를 반환 (편의시설의 name을 반환)
    private List<Facility> getFacilities(String word, Building building) {
        List<Facility> facilities;

        if(building != null) facilities = facilityRepository.findAllByBuildingAndNameContaining(building, word);
        else facilities = facilityRepository.findByNameContaining(word);

        return facilities.stream()
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(Facility::getName, f -> f, (existing, replacement) -> existing),
                map -> new ArrayList<>(map.values())
            ));
    }

    // 특정 건물 및 타입 리스트에 속하는 편의시설 검색
    private List<Facility> getFacilitiesByBuildingAndTypes(Building building, List<FacilityType> types) {
        return facilityRepository.findAllByBuildingAndTypeInOrderByFloor(building, types);
    }

    // 특정 건물 및 타입에 속하는 편의시설 검색 (화장실 검색하는 경우 - 다른 화장실 모두 포함하도록)
    private List<Facility> getFacilitiesByBuildingAndType(Building building, FacilityType facilityType) {
        List<Facility> facilities = facilityRepository.findAllByBuildingAndType(building, facilityType);
        if(facilityType == FacilityType.TOILET) {
            facilities.addAll(facilityRepository.findAllByBuildingAndTypeIn(building, List.of(FacilityType.MEN_TOILET, FacilityType.WOMEN_TOILET, FacilityType.MEN_HANDICAPPED_TOILET, FacilityType.WOMEN_HANDICAPPED_TOILET)));
        }
        else if(facilityType == FacilityType.WOMEN_TOILET) {
            facilities.addAll(facilityRepository.findAllByBuildingAndType(building, FacilityType.WOMEN_HANDICAPPED_TOILET));
        }
        else if(facilityType == FacilityType.MEN_TOILET) {
            facilities.addAll(facilityRepository.findAllByBuildingAndType(building, FacilityType.MEN_HANDICAPPED_TOILET));
        }

        facilities.sort(Comparator.comparing(Facility::getFloor));

        return facilities;
    }

    // 특정 타입에 속하는 편의시설 검색 (화장실 검색하는 경우 - 다른 화장실 모두 포함하도록)
    private List<Facility> getFacilitiesByType(FacilityType facilityType) {
        List<Facility> facilities = facilityRepository.findAllByType(facilityType);
        if(facilityType == FacilityType.TOILET) {
            facilities.addAll(facilityRepository.findAllByTypeIn(List.of(FacilityType.MEN_TOILET, FacilityType.WOMEN_TOILET, FacilityType.MEN_HANDICAPPED_TOILET, FacilityType.WOMEN_HANDICAPPED_TOILET)));
        }
        else if(facilityType == FacilityType.WOMEN_TOILET) {
            facilities.addAll(facilityRepository.findAllByType(FacilityType.WOMEN_HANDICAPPED_TOILET));
        }
        else if(facilityType == FacilityType.MEN_TOILET) {
            facilities.addAll(facilityRepository.findAllByType(FacilityType.MEN_HANDICAPPED_TOILET));
        }

        return facilities;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
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
