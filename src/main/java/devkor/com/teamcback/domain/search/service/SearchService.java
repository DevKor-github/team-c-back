package devkor.com.teamcback.domain.search.service;

import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.repository.BookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import devkor.com.teamcback.domain.building.repository.BuildingNicknameRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceNickname;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.place.repository.PlaceImageRepository;
import devkor.com.teamcback.domain.place.repository.PlaceNicknameRepository;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.search.dto.request.SaveSearchLogReq;
import devkor.com.teamcback.domain.search.dto.response.*;
import devkor.com.teamcback.domain.search.entity.Koyeon;
import devkor.com.teamcback.domain.search.entity.SearchLog;
import devkor.com.teamcback.domain.search.repository.KoyeonRepository;
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
    private final PlaceNicknameRepository placeNicknameRepository;
    private final PlaceRepository placeRepository;
    private final RedisTemplate<String, SearchLog> searchLogRedis;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookmarkRepository bookmarkRepository;
    private final KoyeonRepository koyeonRepository;
    private final PlaceImageRepository placeImageRepository;

    // 점수 계산을 위한 상수
    static final int BASE_SCORE_BUILDING_DEFAULT = 1000;
    static final int BASE_SCORE_BUILDING_WITH_KEYWORD = -500;
    static final int BASE_SCORE_FACILITY_DEFAULT = 500;
    static final int BASE_SCORE_FACILITY_SPECIAL = 0;
    static final int BASE_SCORE_CLASSROOM_DEFAULT = 0;

    // 아이콘으로 표시할 편의시설 종류
    private final List<PlaceType> iconTypes = Arrays.asList(PlaceType.VENDING_MACHINE, PlaceType.PRINTER, PlaceType.LOUNGE,
        PlaceType.READING_ROOM, PlaceType.STUDY_ROOM, PlaceType.CAFE, PlaceType.CONVENIENCE_STORE, PlaceType.CAFETERIA,
        PlaceType.SLEEPING_ROOM, PlaceType.SHOWER_ROOM, PlaceType.BANK, PlaceType.GYM);

    /**
     * 고연전 여부 확인
     */
    @Transactional(readOnly = true)
    public Koyeon isKoyeon() {
        return koyeonRepository.findById(1L).orElseThrow(() -> new GlobalException(NOT_FOUND_KOYEON));
    }

    /**
     * 통합 검색
     */
    @Transactional(readOnly = true)
    public GlobalSearchListRes globalSearch(String word) {
        List<GlobalSearchRes> list = new ArrayList<>();
        Map<GlobalSearchRes, Integer> scores = new HashMap<>();

        List<Place> places;
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
        places = getPlaces(word, null);

        for(Building building : buildings) {
            list.add(new GlobalSearchRes(building, LocationType.BUILDING));
        }
        for(Place place : places) {
            list.add(new GlobalSearchRes(place, LocationType.PLACE, false));
        }

        return new GlobalSearchListRes(orderSequence(calculateScore(list, word, null)));
    }

    /**
     * 모든 or 편의시설에 해당하는 건물 검색
     */
    @Transactional(readOnly = true)
    public SearchBuildingListRes searchBuildings(PlaceType type) {
        List<Building> buildingList;
        if(type == null) {
            buildingList = buildingRepository.findAll();

            buildingList = buildingList.stream()
                .filter(building -> building.getId() != 0).toList();
        }
        else {
            List<Place> placeList = getFacilitiesByType(type);
            buildingList = placeList.stream()
                .map(Place::getBuilding)
                .distinct()
                .toList();
        }

        return new SearchBuildingListRes(
            buildingList.stream()
            .map(building -> {
                List<PlaceType> containPlaceTypes = getFacilitiesByBuildingAndTypes(building, iconTypes).stream()
                    .map(Place::getType)
                    .distinct()
                    .toList();
                return new SearchBuildingRes(building, containPlaceTypes);
            })
            .toList());
    }

    /**
     * 건물 내 특정 종류의 편의시설 검색
     */
    @Transactional(readOnly = true)
    public SearchBuildingFacilityListRes searchBuildingFacilityByType(Long buildingId, PlaceType placeType) {
        Building building = findBuilding(buildingId);
        SearchBuildingFacilityListRes res = new SearchBuildingFacilityListRes(building, placeType);

        List<Place> facilities = getFacilitiesByBuildingAndType(building, placeType);

        Map<Double, List<SearchFacilityRes>> map = new HashMap<>();
        for(Place place : facilities) {
            if(!map.containsKey(place.getFloor())) map.put(place.getFloor(), new ArrayList<>());
            map.get(place.getFloor()).add(new SearchFacilityRes(place));
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
        List<Place> facilities = placeRepository.findAllByBuilding(building);
        List<PlaceType> placeTypeList = facilities.stream()
            .map(Place::getType)
            .filter(placeType -> placeType != PlaceType.CLASSROOM)
            .distinct()
            .toList();

        return new SearchFacilityTypeRes(placeTypeList);
    }

    /**
     * 건물 특정 층에 있는 장소 검색
     */
    @Transactional(readOnly = true)
    public SearchRoomRes searchPlaceByBuildingFloor(Long buildingId, int floor) {
         Building building = findBuilding(buildingId);
         List<Place> placeList = placeRepository.findAllByBuildingAndFloor(building, floor);

         List<SearchRoomDetailRes> roomDetailRes = new ArrayList<>(
             placeList.stream().map(SearchRoomDetailRes::new).toList());
         roomDetailRes.addAll(placeList.stream().map(SearchRoomDetailRes::new).toList());

         return new SearchRoomRes(roomDetailRes);
    }

    /**
     * 타입별 편의시설 목록 조회
     */
    @Transactional(readOnly = true)
    public SearchFacilityListRes searchFacilitiesWithType(PlaceType placeType) {
        // TODO: 위경도 값이 null인 facility 예외 처리
        List<SearchPlaceRes> placeList = placeRepository.findAllByType(placeType).stream().map(SearchPlaceRes::new).toList();

        return new SearchFacilityListRes(placeList);
    }

    /**
     * Mask Index 대응 교실 조회
     */
    @Transactional(readOnly = true)
    public SearchPlaceByMaskIndexRes searchPlaceByMaskIndex(Long buildingId, int floor, Integer maskIndex) {
        Building building = findBuilding(buildingId);
        Place place = placeRepository.findByBuildingAndFloorAndMaskIndex(building, floor, maskIndex);
        if(place == null) {
            throw new GlobalException(NOT_FOUND_PLACE);
        }
        return new SearchPlaceByMaskIndexRes(place);
    }

    /**
     * 교실 대응 Mask Index 조회
     */
    @Transactional(readOnly = true)
    public SearchMaskIndexByPlaceRes searchMaskIndexByPlace(Long placeId) {
        return new SearchMaskIndexByPlaceRes(findPlace(placeId));
    }

    /**
     * 건물 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public SearchBuildingDetailRes searchBuildingDetail(Long userId, Long buildingId) {
        Building building = findBuilding(buildingId);

        //(임의) 가져올 대표 시설 정보 List (라운지, 카페, 편의점, 식당, 헬스장, 열람실, 스터디룸, 수면실, 샤워실)
        //자세한 회의 후 List 수정하기
        List<PlaceType> types = Arrays.asList(PlaceType.LOUNGE, PlaceType.CAFE, PlaceType.CONVENIENCE_STORE, PlaceType.CAFETERIA, PlaceType.READING_ROOM, PlaceType.STUDY_ROOM, PlaceType.GYM, PlaceType.SLEEPING_ROOM, PlaceType.SHOWER_ROOM);
        List<Place> mainFacilities = getFacilitiesByBuildingAndTypes(building, types);
        List<SearchMainFacilityRes> res = new ArrayList<>();

        for (Place place : mainFacilities) {
            res.add(new SearchMainFacilityRes(place));
        }

        List<PlaceType> containPlaceTypes = getFacilitiesByBuildingAndTypes(building, iconTypes).stream()
            .map(Place::getType)
            .distinct()
            .toList();

        //즐겨찾기 여부 확인 (로그인 X -> false)
        boolean bookmarked = false;
        if(userId != null) {
            User user = findUser(userId);
            // 해당 유저의 북마크에 빌딩 있는지 확인 (유저의 카테고리 리스트 가져와서, 해당 안에 존재하는지 확인)
            List<Category> categories = categoryRepository.findAllByUser(user);
            if(bookmarkRepository.existsByLocationTypeAndLocationIdAndCategoryIn(LocationType.BUILDING, buildingId, categories)) {
                bookmarked = true;
            }
        }

        // TODO: 커뮤니티 구상 완료되면 커뮤니티 정보 넣기

        return new SearchBuildingDetailRes(res, containPlaceTypes, building, bookmarked);
    }

    /**
     * 장소(교실, 편의시설) 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public SearchPlaceDetailRes searchPlaceDetail(Long userId, Long placeId) {
        List<Category> categories = new ArrayList<>();
        boolean bookmarked = false;

        //즐겨찾기 여부 확인용 정보 가져오기
        if(userId != null) {
            User user = findUser(userId);
            categories = categoryRepository.findAllByUser(user);
        }

        Place place = findPlace(placeId);
        if(bookmarkRepository.existsByLocationTypeAndLocationIdAndCategoryIn(LocationType.PLACE, place.getId(), categories)) {
            bookmarked = true;
        }
        return new SearchPlaceDetailRes(place, bookmarked, placeImageRepository.findAllByPlace(place).stream().map(SearchPlaceImageRes::new).toList());
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

        List<SearchLog> existingLogs = searchLogRedis.opsForList().range(key, 0, -1);

        // 중복되는 기존 로그 삭제
        if (existingLogs != null) {
            for (SearchLog log : existingLogs) {
                if (log.getId().equals(req.getId()) && log.getType().equals(req.getType())) {
                    searchLogRedis.opsForList().remove(key, 1, log);
                    break; // 이미 기존에 중복값이 없으므로 첫 번째만 제거
                }
            }
        }

        Long size = searchLogRedis.opsForList().size(key);

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
        List<Place> places;

        for (Building building : buildings) {
            list.add(new GlobalSearchRes(building, LocationType.BUILDING));
            places = getPlaces(placeWord, building);

            for (Place place : places) {
                list.add(new GlobalSearchRes(place, LocationType.PLACE, true));
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
            if (res.getLocationType() == LocationType.BUILDING) {
                baseScore = buildingKeyword == null ? BASE_SCORE_BUILDING_DEFAULT : BASE_SCORE_BUILDING_WITH_KEYWORD;
                indexScore = buildingKeyword != null ? calculateScoreByIndex(res.getName(), buildingKeyword) : calculateScoreByIndex(res.getName(), keyword);
            }
            if (res.getLocationType() == LocationType.PLACE) {
                if(res.getPlaceType().equals(PlaceType.CLASSROOM)) {
                    baseScore = BASE_SCORE_CLASSROOM_DEFAULT;
                    indexScore = calculateScoreByIndex(res.getName(), keyword);
                } else {
                    if (res.getName().contains(" ")) {
                        baseScore = checkFacilityType(res.getName().split(" ", 2)[1]) ? BASE_SCORE_FACILITY_DEFAULT : BASE_SCORE_FACILITY_SPECIAL;
                    } else {
                        baseScore = BASE_SCORE_FACILITY_DEFAULT;
                    }
                    indexScore = calculateScoreByIndex(res.getName(), keyword);
                }
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
        return Arrays.stream(PlaceType.values())
            .anyMatch(facilityType -> facilityType.getName().equals(name));
    }

    private static List<PlaceType> getFacilityType(String name) {
        return Arrays.stream(PlaceType.values())
            .filter(facilityType -> facilityType.getName().contains(name))
            .toList();
    }


    private int calculateScoreByIndex(String name, String keyword) {
        // 괄호() > 대학 > 관 (앞의 것이 존재한다면 해당 것을 기준으로 삼음)
        String[] criteria = {"\\(", "대학", "관"};
        int indexScore = 0;

        for (String c : criteria) {
            if(name.contains(c.replace("\\", ""))) {
                String[] temp = name.split(c, 2);
                // 기준 부분이 keyword를 포함한다면 이를 새로운 name으로 설정
                if(!temp[1].isEmpty() && temp[1].contains(String.valueOf(keyword.charAt(0)))) {
                    name = temp[1];
                    break;
                }
            }
        }

        int index;
        if(keyword.contains(" ")) {
            String[] keywords = keyword.split(" ", 2);
            int index1 = name.indexOf(keywords[0].charAt(0));
            int index2 = name.indexOf(keywords[1].charAt(0));

            index = (index1 == -1) ? index2 : (index2 == -1) ? index1 : Math.min(index1, index2);
        } else {
            index = name.indexOf(keyword.charAt(0));
        }
        if (index != -1) indexScore += (100 - index);

        return indexScore;
    }

    private List<Place> getPlaces(String word, Building building) {
        List<Place> places = new ArrayList<>();
        if(building != null) {
            places = placeRepository.findAllByBuilding(building);
        }

        //응답 개수 제한
        Pageable limit = PageRequest.of(0, 30, Sort.by("id").descending());

        // 강의실 조회
        List<PlaceNickname> placeNicknames = new ArrayList<>();
        if(building != null && !places.isEmpty()) {
            placeNicknames = placeNicknameRepository.findByNicknameContainingAndPlaceInOrderByNickname(word, places, limit);
        }
        else if(building == null) {
            placeNicknames = placeNicknameRepository.findByNicknameContainingOrderByNickname(word, limit);
        }

        // 중복을 제거하여 List에 저장
        return placeNicknames.stream()
            .map(PlaceNickname::getPlace)
            .distinct()
            .toList();
    }

    // 특정 건물 및 타입 리스트에 속하는 편의시설 검색
    private List<Place> getFacilitiesByBuildingAndTypes(Building building, List<PlaceType> types) {
        return placeRepository.findAllByBuildingAndTypeInOrderByFloor(building, types);
    }

    // 특정 건물 및 타입에 속하는 편의시설 검색 (화장실 검색하는 경우 - 다른 화장실 모두 포함하도록)
    private List<Place> getFacilitiesByBuildingAndType(Building building, PlaceType placeType) {
        List<Place> facilities = placeRepository.findAllByBuildingAndType(building, placeType);
        if(placeType == PlaceType.TOILET) {
            facilities.addAll(placeRepository.findAllByBuildingAndTypeIn(building, List.of(PlaceType.MEN_TOILET, PlaceType.WOMEN_TOILET, PlaceType.MEN_HANDICAPPED_TOILET, PlaceType.WOMEN_HANDICAPPED_TOILET)));
        }
        else if(placeType == PlaceType.WOMEN_TOILET) {
            facilities.addAll(placeRepository.findAllByBuildingAndType(building, PlaceType.WOMEN_HANDICAPPED_TOILET));
        }
        else if(placeType == PlaceType.MEN_TOILET) {
            facilities.addAll(placeRepository.findAllByBuildingAndType(building, PlaceType.MEN_HANDICAPPED_TOILET));
        }

        facilities.sort(Comparator.comparing(Place::getFloor));

        return facilities;
    }

    // 특정 타입에 속하는 편의시설 검색 (화장실 검색하는 경우 - 다른 화장실 모두 포함하도록)
    private List<Place> getFacilitiesByType(PlaceType placeType) {
        List<Place> facilities = placeRepository.findAllByType(placeType);
        if(placeType == PlaceType.TOILET) {
            facilities.addAll(placeRepository.findAllByTypeIn(List.of(PlaceType.MEN_TOILET, PlaceType.WOMEN_TOILET, PlaceType.MEN_HANDICAPPED_TOILET, PlaceType.WOMEN_HANDICAPPED_TOILET)));
        }
        else if(placeType == PlaceType.WOMEN_TOILET) {
            facilities.addAll(placeRepository.findAllByType(PlaceType.WOMEN_HANDICAPPED_TOILET));
        }
        else if(placeType == PlaceType.MEN_TOILET) {
            facilities.addAll(placeRepository.findAllByType(PlaceType.MEN_HANDICAPPED_TOILET));
        }

        return facilities;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }

    private Building findBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId).orElseThrow(() -> new GlobalException(NOT_FOUND_BUILDING));
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
    }
}
