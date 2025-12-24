package devkor.com.teamcback.domain.search.service;

import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.repository.BookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import devkor.com.teamcback.domain.building.repository.BuildingNicknameRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.common.util.FileUtil;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceNickname;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.place.repository.PlaceImageRepository;
import devkor.com.teamcback.domain.place.repository.PlaceNicknameRepository;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.routes.repository.NodeRepository;
import devkor.com.teamcback.domain.search.dto.request.SaveSearchLogReq;
import devkor.com.teamcback.domain.search.dto.response.*;
import devkor.com.teamcback.domain.search.entity.DefaultPlace;
import devkor.com.teamcback.domain.search.entity.SearchLog;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.logging.LogUtil;
import devkor.com.teamcback.global.response.CursorPageRes;
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

import static devkor.com.teamcback.domain.routes.entity.NodeType.*;
import static devkor.com.teamcback.domain.search.util.HangeulUtils.*;
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
    private final PlaceImageRepository placeImageRepository;
    private final NodeRepository nodeRepository;
    private final LogUtil logUtil;
    private final FileUtil fileUtil;

    // 점수 계산을 위한 상수
    static final int BASE_SCORE_BUILDING_DEFAULT = 1000;
    static final int BASE_SCORE_BUILDING_WITH_KEYWORD = -500;
    static final int BASE_SCORE_FACILITY_DEFAULT = 500;
    static final int BASE_SCORE_FACILITY_SPECIAL = 0;
    static final int BASE_SCORE_CLASSROOM_DEFAULT = 0;
    static final int BASE_SCORE_IS_BOOKMARKED = 5000;
    static final int BASE_SCORE_OUTDOOR_TAG_DEFAULT = 750;

    // 건물 상세 모달 아이콘으로 표시할 편의시설 종류
    private static final List<PlaceType> iconTypes = Arrays.asList(PlaceType.VENDING_MACHINE, PlaceType.PRINTER, PlaceType.LOUNGE,
        PlaceType.READING_ROOM, PlaceType.STUDY_ROOM, PlaceType.CAFE, PlaceType.CONVENIENCE_STORE, PlaceType.CAFETERIA,
        PlaceType.SLEEPING_ROOM, PlaceType.SHOWER_ROOM, PlaceType.BANK, PlaceType.GYM);

    // 통합 검색 결과에 표시하지 않을 편의시설 종류
    //TODO: 자전거보관소, 벤치 디자인 요청 후 List에서 제거
    private static final List<String> excludedTypes = Arrays.asList(PlaceType.CLASSROOM.getName(), PlaceType.TOILET.getName(),
        PlaceType.MEN_TOILET.getName(), PlaceType.WOMEN_TOILET.getName(), PlaceType.MEN_HANDICAPPED_TOILET.getName(),
        PlaceType.WOMEN_HANDICAPPED_TOILET.getName(), PlaceType.LOCKER.getName(), PlaceType.BICYCLE_RACK.getName(), PlaceType.BENCH.getName());

    // 통합 검색 결과에서 "건물명 + 기본편의시설명"의 형태로 제공되어야 하는 편의시설 종류
    private static final List<PlaceType> outerTagTypes = Arrays.asList(PlaceType.CAFE, PlaceType.CAFETERIA, PlaceType.CONVENIENCE_STORE,
        PlaceType.READING_ROOM, PlaceType.STUDY_ROOM, PlaceType.BOOK_RETURN_MACHINE, PlaceType.LOUNGE, PlaceType.WATER_PURIFIER, PlaceType.REUSABLE_CUP_RETURN,
        PlaceType.VENDING_MACHINE, PlaceType.PRINTER, PlaceType.TUMBLER_WASHER, PlaceType.ONESTOP_AUTO_MACHINE, PlaceType.BANK, PlaceType.TRASH_CAN,
        PlaceType.SMOKING_BOOTH, PlaceType.SHOWER_ROOM, PlaceType.GYM, PlaceType.SLEEPING_ROOM, PlaceType.HEALTH_OFFICE, PlaceType.DISABLED_PARKING);

    // 건물 상세 조회 : 대표 편의시설 종류
    private static final List<PlaceType> mainFacilityTypes = Arrays.asList(PlaceType.LOUNGE, PlaceType.CAFE, PlaceType.CONVENIENCE_STORE, PlaceType.CAFETERIA,
        PlaceType.READING_ROOM, PlaceType.STUDY_ROOM, PlaceType.GYM, PlaceType.SLEEPING_ROOM, PlaceType.SHOWER_ROOM);

    /**
     * 통합 검색
     */
    @Transactional(readOnly = true)
    public GlobalSearchListRes globalSearch(String word, Long userId) {
        List<GlobalSearchRes> list = new ArrayList<>();

        List<Place> places;
        List<Building> buildings;

        User user = userId != null ? findUser(userId) : null;

        // 전체 키워드 검색
        buildings = getBuildings(word);
        places = getPlaces(word, null);

        for(Building building : buildings) {
            list.add(new GlobalSearchRes(building, LocationType.BUILDING, checkBookmarked(user, building)));
        }
        for(Place place : places) {
            if(!excludedTypes.contains(place.getName())) {
                list.add(new GlobalSearchRes(place, LocationType.PLACE, false, checkBookmarked(user, place)));
            }
        }
        Map<GlobalSearchRes, Integer> scores = new HashMap<>(calculateScore(list, word, null));

        // 건물이 입력됨
        if(word.contains(" ")) {
            // 건물 + 장소 조합 (건물명이 앞에 있는 경우)
            String firstBuildingWord = word.split(" ")[0];
            String firstPlaceWord = word.substring(firstBuildingWord.length()).trim();

            buildings = getBuildings(firstBuildingWord);
            if(!buildings.isEmpty()) {
                scores.putAll(getScores(word, buildings, firstPlaceWord, firstBuildingWord, user));
            }

            // 장소 + 건물 조합 (건물명이 뒤에 있는 경우)
            String lastBuildingWord = word.split(" ")[word.split(" ").length - 1];
            String lastPlaceWord = word.substring(0, word.length() - lastBuildingWord.length()).trim();

            buildings = getBuildings(lastBuildingWord);
            if(!buildings.isEmpty()) {
                scores.putAll(getScores(word, buildings, lastPlaceWord, lastBuildingWord, user));
            }
        }

        logUtil.logSearch(word);
        return new GlobalSearchListRes(orderSequence(scores));
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
            logUtil.logClick(null, null, null, type.toString());
        }

        List<SearchBuildingRes> resList = new ArrayList<>();
        for(Building building : buildingList) {
            // 편의시설 종류
            List<PlaceType> containPlaceTypes = getFacilitiesByBuildingAndTypes(building, iconTypes).stream().map(Place::getType).distinct().toList();

            // 건물 대표 사진
            String imageUrl = null;
            if(building.getFileUuid() != null) {
                imageUrl = fileUtil.getThumbnail(building.getFileUuid());
            }

            resList.add(new SearchBuildingRes(building, imageUrl, containPlaceTypes));
        }

        return new SearchBuildingListRes(resList);
    }

    /**
     * 건물 내 특정 종류의 편의시설 검색
     */
    @Transactional(readOnly = true)
    public SearchBuildingFacilityListRes searchBuildingFacilityByType(Long buildingId, PlaceType placeType) {
        Building building = findBuilding(buildingId);
        SearchBuildingFacilityListRes res = new SearchBuildingFacilityListRes(building);

        List<Place> facilities = getFacilitiesByBuildingAndType(building, placeType);

        Map<Double, List<SearchFacilityRes>> map = new HashMap<>();
        for(Place place : facilities) {
            if(!map.containsKey(place.getFloor())) map.put(place.getFloor(), new ArrayList<>());

            String imageUrl = null;
            if(place.getFileUuid() != null) {
                imageUrl = fileUtil.getThumbnail(place.getFileUuid());
            }
            map.get(place.getFloor()).add(new SearchFacilityRes(place, imageUrl));
        }

        res.setFacilities(map);

        logUtil.logClick(building.getName(), null, null, placeType.toString());
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
    public SearchFloorInfoRes searchPlaceByBuildingFloor(Long buildingId, int floor) {
         Building building = findBuilding(buildingId);
         List<Place> placeList = placeRepository.findAllByBuildingAndFloor(building, floor);
         List<SearchRoomDetailRes> placeResList = new ArrayList<>();

         for(Place place : placeList) {
             String imageUrl = null;
             if(place.getFileUuid() != null) {
                 imageUrl = fileUtil.getThumbnail(place.getFileUuid());
             }

             placeResList.add(new SearchRoomDetailRes(place, imageUrl));
         }

        List<SearchNodeRes> nodeList = nodeRepository.findAllByBuildingAndFloorAndTypeIn(building, floor, List.of(ENTRANCE, STAIR, ELEVATOR))
            .stream().map(SearchNodeRes::new).toList();

//         List<SearchNodeRes> nodeList = nodeRepository.findAllByBuildingAndFloorAndTypeIn(building, floor, List.of(ENTRANCE, STAIR, ELEVATOR))
//             .stream()
//             .filter(node -> { // 출입구 중 placeList에 존재하지 않는 것들만 추가
//                 if (node.getType() != ENTRANCE) return true;
//                 return placeList.stream()
//                     .noneMatch(p -> p.getNode().getId().equals(node.getId()));
//             })
//             .map(SearchNodeRes::new).toList();

         return new SearchFloorInfoRes(placeResList, nodeList);
    }

    /**
     * 타입별 편의시설 목록 조회
     */
    @Transactional(readOnly = true)
    public SearchFacilityListRes searchFacilitiesWithType(PlaceType placeType) {
        // TODO: 위경도 값이 null인 facility 예외 처리
        List<Place> placeList = placeRepository.findAllByType(placeType);

        //임시코드: 갯수제한 및 셔플
//        if(placeType == PlaceType.CAFT_TEMP) {
//            Collections.shuffle(placeList);
//            placeList = placeList.subList(0, 50);
//        }

        List<SearchPlaceRes> placeResList = new ArrayList<>();

        for(Place place : placeList) {
            String imageUrl = null;
            if(place.getFileUuid() != null) {
                imageUrl = fileUtil.getThumbnail(place.getFileUuid());
            }

            placeResList.add(new SearchPlaceRes(place, imageUrl));
        }
        logUtil.logClick(null, null, null, placeType.toString());
        return new SearchFacilityListRes(placeResList);
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
        logUtil.logClick(building.getName(), place.getName(), place.getFloor(), place.getType().toString());
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

        // TODO: 배포 버전 수정 후 편의시설 조회 제거
        List<Place> mainFacilities = getFacilitiesByBuildingAndTypes(building, mainFacilityTypes);
        List<SearchMainFacilityRes> res = new ArrayList<>();

        for (Place place : mainFacilities) {
            String imageUrl = null;
            if(place.getFileUuid() != null) {
                imageUrl = fileUtil.getThumbnail(place.getFileUuid());
            }
            if(place.getImageUrl() == null) {
                place.setImageUrl(DefaultPlace.getUrlByPlaceType(place.getType()));
            }
            res.add(new SearchMainFacilityRes(place, imageUrl));
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
            if(bookmarkRepository.existsByLocationIdAndLocationTypeAndCategoryBookmarkList_CategoryIn(buildingId, LocationType.BUILDING, categories)) {
                bookmarked = true;
            }
        }

        // 건물 대표 이미지 확인
        String imageUrl = null;
        if(building.getFileUuid() != null) {
            imageUrl = fileUtil.getThumbnail(building.getFileUuid());
        }
        logUtil.logClick(building.getName(), null, null, null);
        return new SearchBuildingDetailRes(res, containPlaceTypes, building, imageUrl, bookmarked);
    }

    /**
     * 건물 대표 편의시설 목록 조회
     */
    @Transactional(readOnly = true)
    public CursorPageRes<SearchMainFacilityRes> searchBuildingMainFacilityList(Long buildingId, Long lastPlaceId, int size) {
        Place lastPlace = (lastPlaceId == null) ? null : findPlace(lastPlaceId);

        List<Place> placeList = placeRepository.getFacilitiesByBuildingAndTypesWithPage(buildingId, mainFacilityTypes, lastPlace, size + 1);
        List<SearchMainFacilityRes> mainFacilities = new ArrayList<>();

        for (Place place : placeList) {
            String imageUrl = null;
            if(place.getFileUuid() != null) {
                imageUrl = fileUtil.getThumbnail(place.getFileUuid());
            }
            mainFacilities.add(new SearchMainFacilityRes(place, imageUrl));
        }

        boolean hasNext = mainFacilities.size() > size;
        if (hasNext) mainFacilities.remove(size);

        Long lastCursorId = mainFacilities.isEmpty() ? null : mainFacilities.get(mainFacilities.size() - 1).getPlaceId();

        return new CursorPageRes<>(mainFacilities, hasNext, lastCursorId);
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

        // 장소
        Place place = findPlace(placeId);

        // 장소 사진
        // TODO: 나중에 수정
        String imageUrl = null;
        List<SearchPlaceImageRes> placeImageList;
        if(place.getFileUuid() != null) {
            placeImageList = fileUtil.getThumbnailFiles(place.getFileUuid()).stream().map(image -> new SearchPlaceImageRes(0L, image)).toList();
            imageUrl = placeImageList.isEmpty() ? null : placeImageList.get(0).getImage();
        }
        else {
            placeImageList = new ArrayList<>();placeImageRepository.findAllByPlace(place).stream().map(SearchPlaceImageRes::new).toList();
        }

        // 즐겨찾기
        if(bookmarkRepository.existsByLocationIdAndLocationTypeAndCategoryBookmarkList_CategoryIn(place.getId(), LocationType.PLACE, categories)) {
            bookmarked = true;
        }
        logUtil.logClick(place.getBuilding().getName(), place.getName(), place.getFloor(), place.getType().toString());
        return new SearchPlaceDetailRes(place, imageUrl, bookmarked, placeImageList);
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
        SearchLog searchLog = new SearchLog(req, searchedAt);
        String key = String.valueOf(userId);

        List<SearchLog> existingLogs = searchLogRedis.opsForList().range(key, 0, -1);

        // 중복되는 기존 로그 삭제
        if (existingLogs != null) {
            for (SearchLog log : existingLogs) {
                if (log.getId().equals(req.getId()) && log.getLocationType().equals(req.getLocationType())) {
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

    private List<Building> getBuildings(String tempWord) {
        String word = tempWord.replace(" ", "");
        // 건물 조회
        List<BuildingNickname> buildingNicknames = buildingNicknameRepository.findAllByJasoDecomposeContaining(decomposeHangulString(word));
        if(isConsonantOnly(word)) buildingNicknames.addAll(buildingNicknameRepository.findAllByChosungContaining(extractChosung(word)));

        // 중복을 제거하여 List에 저장
        return buildingNicknames.stream()
            .map(BuildingNickname::getBuilding)
            .distinct()
            .toList();
    }

    private Map<GlobalSearchRes, Integer> getScores(String word, List<Building> buildings, String placeWord, String buildingWord, User user) {
        List<GlobalSearchRes> list = new ArrayList<>();
        List<Place> places;

        for (Building building : buildings) {
            // 빌딩 정보 추가
            list.add(new GlobalSearchRes(building, LocationType.BUILDING, checkBookmarked(user, building)));
            places = getPlaces(placeWord, building);

            for (Place place : places) {
                // 해당 빌딩의 장소 정보 추가
                checkBookmarked(user, place);
                if(!excludedTypes.contains(place.getName())) {
                    list.add(new GlobalSearchRes(place, LocationType.PLACE, true, checkBookmarked(user, place)));
                }
            }
        }
        return calculateScore(list, word, buildingWord);
    }

    private Category checkBookmarked(User user, Building building) {
        if(user == null) return null;
        List<Category> categories = categoryRepository.findCategoriesByUserAndLocationTypeAndLocationId(user, LocationType.BUILDING, building.getId());
        return categories.isEmpty() ? null : categories.get(0);
    }

    private Category checkBookmarked(User user, Place place) {
        if(user == null) return null;
        List<Category> categories = categoryRepository.findCategoriesByUserAndLocationTypeAndLocationId(user, LocationType.PLACE, place.getId());
        return categories.isEmpty() ? null : categories.get(0);
    }

    private Map<GlobalSearchRes, Integer> calculateScore(List<GlobalSearchRes> list, String keyword, String buildingKeyword) {
        Map<GlobalSearchRes, Integer> scores = new HashMap<>();
        // 강의실, 특수명 편의시설은 baseScore = 0
        int baseScore = 0;
        int indexScore = 0;
        int bookmarkScore;

        for (GlobalSearchRes res : list) {
            bookmarkScore = res.isBookmarked() ? BASE_SCORE_IS_BOOKMARKED : 0;
            if (res.getLocationType() == LocationType.BUILDING) {
                // 건물+장소명인 경우 기본 "건물명"은 맨 밑으로
                baseScore = buildingKeyword == null ? BASE_SCORE_BUILDING_DEFAULT : BASE_SCORE_BUILDING_WITH_KEYWORD;
                indexScore = calculateScoreByIndex(res.getName(), keyword);
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
            if (res.getLocationType() == LocationType.FACILITY) {
                baseScore = BASE_SCORE_OUTDOOR_TAG_DEFAULT;
                indexScore = calculateScoreByIndex(res.getName(), keyword);
            }
            scores.put(res, baseScore + indexScore + bookmarkScore);
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

    private int calculateScoreByIndex(String originalName, String keyword) {
        // 괄호() > 대학 > 관 (앞의 것이 존재한다면 해당 것을 기준으로 삼음)
        String[] criteria = {"\\(", "대학", "관"};
        for (String c : criteria) {
            if(originalName.contains(c.replace("\\", ""))) {
                String[] temp = originalName.split(c, 2);
                // 기준 부분이 keyword를 포함한다면 이를 새로운 name으로 설정
                if(!temp[1].isEmpty() && temp[1].contains(String.valueOf(keyword.charAt(0)))) {
                    originalName = temp[1];
                    break;
                }
            }
        }

        int index;
        if(keyword.contains(" ")) {
            String[] keywords = keyword.split(" ", 2);
            int index1 = originalName.indexOf(keywords[0].charAt(0)); // building index 값
            int index2 = originalName.indexOf(keywords[1].charAt(0)); // place index 값

            index = (index1 == -1) ? 100 - index2 : (index2 == -1) ? 100 - index1 : 200 - index1 - index2;
        } else {
            int indexVal = originalName.indexOf(keyword.charAt(0));
            index = (indexVal == -1) ? indexVal : 100 - indexVal;
        }
        return index;
    }

    private List<Place> getPlaces(String tempWord, Building building) {
        String word = tempWord.replace(" ", "");
        List<Place> places = new ArrayList<>();
        // Nickname Table에 building 정보가 없기 때문에, 빌딩 제한이 있는 경우 전체를 불러오고 걸러내기
        if(building != null) {
            places = placeRepository.findAllByBuilding(building);
        }

        //응답 개수 제한
        Pageable limit = PageRequest.of(0, 30, Sort.by("id").descending());

        // 강의실 조회
        List<PlaceNickname> placeNicknames;
        List<Place> resultPlaces = new ArrayList<>();
        if(building != null && !places.isEmpty()) { // 빌딩 제한 있는 경우
            placeNicknames = placeNicknameRepository.findByJasoDecomposeContainingAndPlaceInOrderByNickname(decomposeHangulString(word), places, limit);
            // 초성으로만 구성된 경우
            if(isConsonantOnly(word)) placeNicknames.addAll(placeNicknameRepository.findByChosungContainingAndPlaceInOrderByNickname(extractChosung(word), places, limit));

            // 중복을 제거하여 List에 저장
            resultPlaces.addAll(placeNicknames.stream()
                .map(PlaceNickname::getPlace)
                .distinct()
                .toList());

            // 빌딩 + 편의시설명의 경우를 GlobalSearchRes로 추가하기 (Ex. 하나스퀘어 카페)
            // word가 편의시설명과 부분일치하는지 확인하기(outerTagTypes) && 빌딩에 해당 시설이 있는지 확인
            for (PlaceType type : outerTagTypes) {
                if((type.getName().contains(word) || Arrays.stream(type.getNickname()).anyMatch(nickname -> nickname.contains(word)))) {
                    List<Place> tempPlace = placeRepository.findAllByBuildingAndType(building, type);
                    if(!tempPlace.isEmpty()) {
                        resultPlaces.addAll(tempPlace);
                        resultPlaces.add(new Place(type, building));
                    }
                }
            }
        }
        else if(building == null) { // 빌딩 제한 없는 전체 검색
            placeNicknames = placeNicknameRepository.findAllByJasoDecomposeContainingOrderByNickname(decomposeHangulString(word), limit);
            // 초성으로만 구성된 경우
            if(isConsonantOnly(word)) placeNicknames.addAll( placeNicknameRepository.findAllByChosungContainingOrderByNickname(extractChosung(word), limit));

            // 중복을 제거하여 List에 저장
            resultPlaces.addAll(placeNicknames.stream()
                .map(PlaceNickname::getPlace)
                .distinct()
                .toList());

            // 자체가 편의시설명인 경우 : 야외 태그
            for (PlaceType type : outerTagTypes) {
                if(type.getName().contains(word) || Arrays.stream(type.getNickname()).anyMatch(nickname -> nickname.contains(word))) {
                    resultPlaces.add(new Place(type, findBuilding(0L)));
                }
            }
        }
        return resultPlaces;
    }

    // 특정 건물 및 타입 리스트에 속하는 편의시설 검색
    private List<Place> getFacilitiesByBuildingAndTypes(Building building, List<PlaceType> types) {
        return placeRepository.findAllByBuildingAndTypeInAndAvailabilityOrderByFloor(building, types, true);
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
