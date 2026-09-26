package devkor.com.teamcback.domain.chatbot.tool;

import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.AMBIGUOUS_LOCATION;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.INVALID_INPUT;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.NOT_FOUND;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.NO_DATA;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.TEMPORARILY_UNAVAILABLE;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.UNSUPPORTED;
import static devkor.com.teamcback.global.response.ResultCode.COORDINATES_TOO_FAR;
import static devkor.com.teamcback.global.response.ResultCode.COORDINATES_TOO_NEAR;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_DEVICE;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_NODE;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_PLACE;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_ROUTE;
import static devkor.com.teamcback.global.response.ResultCode.NOT_PROVIDED_ROUTE;
import static devkor.com.teamcback.global.response.ResultCode.NOT_SUPPORTED_PLACE_TYPE;
import static devkor.com.teamcback.global.response.ResultCode.NO_DATA_FOR_DEVICE;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolError;
import devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode;
import devkor.com.teamcback.domain.chatbot.tool.dto.CampusStatusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.CrowdLevel;
import devkor.com.teamcback.domain.chatbot.tool.dto.CrowdStatusToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCrowdStatusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCrowdStatusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.CafeteriaMealToolItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.CafeteriaMenuDay;
import devkor.com.teamcback.domain.chatbot.tool.dto.FacilityToolItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindFacilitiesToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindFacilitiesToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCafeteriaMenuToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCafeteriaMenuToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCafeteriaMenuToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetRoomCoursesToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetRoomCoursesToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetRoomCoursesToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetPlaceReviewsToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetPlaceReviewsToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.LocationDetailToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusMatchType;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpoint;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteSectionType;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteStep;
import devkor.com.teamcback.domain.chatbot.tool.dto.RoomCourseToolItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.PlaceReviewsToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.ReviewSummary;
import devkor.com.teamcback.domain.chatbot.tool.dto.ReviewTagSummary;
import devkor.com.teamcback.domain.chatbot.tool.dto.TypicalCrowdPattern;
import devkor.com.teamcback.domain.ble.dto.response.BLETimePatternRes;
import devkor.com.teamcback.domain.ble.dto.response.GetBLERes;
import devkor.com.teamcback.domain.ble.entity.BLEstatus;
import devkor.com.teamcback.domain.ble.service.BLEService;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.common.entity.Weekday;
import devkor.com.teamcback.domain.course.dto.response.GetCourseListRes;
import devkor.com.teamcback.domain.course.dto.response.GetCourseRes;
import devkor.com.teamcback.domain.course.service.CourseService;
import devkor.com.teamcback.domain.place.dto.response.GetCafeteriaMenuListRes;
import devkor.com.teamcback.domain.place.service.CafeteriaMenuService;
import devkor.com.teamcback.domain.routes.dto.response.GetRouteRes;
import devkor.com.teamcback.domain.routes.dto.response.PartialRouteRes;
import devkor.com.teamcback.domain.routes.entity.Conditions;
import devkor.com.teamcback.domain.routes.service.RouteService;
import devkor.com.teamcback.domain.review.dto.response.GetReviewPlaceDetailRes;
import devkor.com.teamcback.domain.review.dto.response.SearchPlaceReviewRes;
import devkor.com.teamcback.domain.review.service.ReviewService;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchRes;
import devkor.com.teamcback.domain.search.dto.response.ChatbotSearchCandidate;
import devkor.com.teamcback.domain.search.dto.response.SearchBuildingDetailRes;
import devkor.com.teamcback.domain.search.dto.response.SearchFacilityRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceDetailRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceRes;
import devkor.com.teamcback.domain.search.dto.response.SearchRoomDetailRes;
import devkor.com.teamcback.domain.search.service.SearchService;
import devkor.com.teamcback.domain.schoolcalendar.service.SchoolCalendarService;
import devkor.com.teamcback.global.exception.exception.AdminException;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.Comparator;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class CampusToolAdapter {
    private static final int CHATBOT_SEARCH_RESULT_LIMIT = 5;
    private final SearchService searchService;
    private final RouteService routeService;
    private final CafeteriaMenuService cafeteriaMenuService;
    private final CourseService courseService;
    private final SchoolCalendarService schoolCalendarService;
    private final BLEService bleService;
    private final ReviewService reviewService;
    private final ChatbotProperties properties;

    public CampusToolAdapter(SearchService searchService, RouteService routeService,
                             CafeteriaMenuService cafeteriaMenuService, CourseService courseService,
                             SchoolCalendarService schoolCalendarService, BLEService bleService,
                             ReviewService reviewService, ChatbotProperties properties) {
        this.searchService = searchService;
        this.routeService = routeService;
        this.cafeteriaMenuService = cafeteriaMenuService;
        this.courseService = courseService;
        this.schoolCalendarService = schoolCalendarService;
        this.bleService = bleService;
        this.reviewService = reviewService;
        this.properties = properties;
    }

    public SearchCampusToolResult searchCampus(SearchCampusToolRequest request) {
        if (request == null || request.query() == null || request.query().trim().isEmpty()) {
            return new SearchCampusToolResult(List.of(), false, error(INVALID_INPUT));
        }
        long startedAt = System.nanoTime();
        int limit = Math.min(CHATBOT_SEARCH_RESULT_LIMIT, properties.tools().search().maxLimit());
        try {
            String query = request.query().trim();
            List<ChatbotSearchCandidate> actualLocations = deduplicate(
                    searchService.chatbotSearch(query, limit));
            String normalizedQuery = normalizeSearchName(query);
            List<RankedSearchCandidate> ranked = actualLocations.stream()
                    .map(item -> new RankedSearchCandidate(item, matchType(normalizedQuery, item.name())))
                    .sorted(Comparator.comparingInt(item -> item.matchType().ordinal()))
                    .toList();
            List<RankedSearchCandidate> exactMatches = ranked.stream()
                    .filter(item -> item.matchType() == SearchCampusMatchType.EXACT).toList();
            List<RankedSearchCandidate> strongMatches = ranked.stream()
                    .filter(item -> item.matchType() == SearchCampusMatchType.STRONG).toList();
            List<RankedSearchCandidate> resolvedLocations = !exactMatches.isEmpty()
                    ? exactMatches : !strongMatches.isEmpty() ? strongMatches : ranked;
            List<SearchCampusItem> candidates = resolvedLocations.stream()
                    .limit(limit)
                    .map(item -> toSearchItem(item.candidate(), item.matchType()))
                    .toList();
            if (candidates.isEmpty()) {
                log.info("chatbot_search query={} limit={} durationMs={} candidateCount=0 ambiguous=false candidates=[]",
                        query, limit, elapsedMillis(startedAt));
                return new SearchCampusToolResult(List.of(), false, error(NOT_FOUND));
            }
            boolean ambiguous = resolvedLocations.size() > 1;
            log.info("chatbot_search query={} limit={} durationMs={} candidateCount={} ambiguous={} candidates={}",
                    query, limit, elapsedMillis(startedAt), candidates.size(), ambiguous,
                    candidates.stream().map(item -> item.name() + ":" + item.locationType() + ":" + item.matchType()).toList());
            return new SearchCampusToolResult(candidates, ambiguous,
                    ambiguous ? error(AMBIGUOUS_LOCATION) : null);
        } catch (GlobalException exception) {
            log.info("chatbot_search query={} limit={} durationMs={} candidateCount=0 ambiguous=false outcome={}",
                    request.query().trim(), limit, elapsedMillis(startedAt), mapDomainError(exception).code());
            return new SearchCampusToolResult(List.of(), false, mapDomainError(exception));
        } catch (RuntimeException exception) {
            log.info("chatbot_search query={} limit={} durationMs={} outcome=TEMPORARILY_UNAVAILABLE",
                    request.query().trim(), limit, elapsedMillis(startedAt));
            return new SearchCampusToolResult(List.of(), false, error(TEMPORARILY_UNAVAILABLE));
        }
    }

    private List<ChatbotSearchCandidate> deduplicate(List<ChatbotSearchCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Map<String, ChatbotSearchCandidate> unique = new LinkedHashMap<>();
        for (ChatbotSearchCandidate candidate : candidates) {
            if (candidate == null || candidate.locationType() == null || candidate.locationId() == null) {
                continue;
            }
            unique.putIfAbsent(candidate.locationType() + ":" + candidate.locationId(), candidate);
        }
        return unique.values().stream().toList();
    }

    public GetLocationDetailToolResult getLocationDetail(GetLocationDetailToolRequest request) {
        if (request == null || request.locationType() == null
                || request.locationId() == null || request.locationId() <= 0) {
            return new GetLocationDetailToolResult(null, error(INVALID_INPUT));
        }
        try {
            LocationDetailToolData data = switch (request.locationType()) {
                case BUILDING -> toBuildingDetail(searchService.searchBuildingDetail(null, request.locationId()));
                case PLACE -> toPlaceDetail(searchService.searchPlaceDetail(null, request.locationId()));
            };
            return new GetLocationDetailToolResult(data, null);
        } catch (GlobalException exception) {
            return new GetLocationDetailToolResult(null, mapDomainError(exception));
        } catch (RuntimeException exception) {
            return new GetLocationDetailToolResult(null, error(TEMPORARILY_UNAVAILABLE));
        }
    }

    public FindFacilitiesToolResult findFacilities(FindFacilitiesToolRequest request) {
        if (!validFacilityRequest(request)) {
            return new FindFacilitiesToolResult(List.of(), error(INVALID_INPUT));
        }
        Integer limit = resolveLimit(request.limit(), properties.tools().facilities());
        if (limit == null) {
            return new FindFacilitiesToolResult(List.of(), error(INVALID_INPUT));
        }

        try {
            List<FacilityToolItem> facilities;
            if (request.facilityType() != null && request.buildingId() == null) {
                facilities = searchService.searchFacilitiesWithType(request.facilityType().toPlaceType())
                        .getFacilities().stream().map(this::toFacilityItem).toList();
            } else if (request.facilityType() != null) {
                facilities = flattenBuildingFacilities(request);
            } else {
                facilities = searchService.searchPlaceByBuildingFloor(request.buildingId(), request.floor())
                        .getRoomList().stream()
                        .map(item -> toFloorItem(item, request.buildingId(), request.floor()))
                        .toList();
            }
            facilities = facilities.stream().limit(limit).toList();
            if (facilities.isEmpty()) {
                return new FindFacilitiesToolResult(List.of(), error(NOT_FOUND));
            }
            return new FindFacilitiesToolResult(facilities, null);
        } catch (GlobalException exception) {
            return new FindFacilitiesToolResult(List.of(), mapDomainError(exception));
        } catch (RuntimeException exception) {
            return new FindFacilitiesToolResult(List.of(), error(TEMPORARILY_UNAVAILABLE));
        }
    }

    public FindRouteToolResult findRoute(FindRouteToolRequest request) {
        if (!validRouteRequest(request)) {
            return new FindRouteToolResult(null, error(INVALID_INPUT));
        }

        try {
            RouteEndpoint start = request.start();
            RouteEndpoint end = request.end();
            List<Conditions> conditions = request.conditions() == null ? List.of()
                    : request.conditions().stream().map(this::toDomainCondition).toList();
            List<GetRouteRes> routes = routeService.findRoute(
                    toDomainType(start), start.locationId(), start.latitude(), start.longitude(),
                    toDomainType(end), end.locationId(), end.latitude(), end.longitude(), conditions);
            if (routes == null || routes.isEmpty()) {
                return new FindRouteToolResult(null, error(NOT_FOUND));
            }
            return new FindRouteToolResult(toRouteData(routes.get(0)), null);
        } catch (AdminException exception) {
            return new FindRouteToolResult(null, error(TEMPORARILY_UNAVAILABLE));
        } catch (GlobalException exception) {
            return new FindRouteToolResult(null, mapRouteError(exception));
        } catch (RuntimeException exception) {
            return new FindRouteToolResult(null, error(TEMPORARILY_UNAVAILABLE));
        }
    }

    private boolean validRouteRequest(FindRouteToolRequest request) {
        if (request == null || !validEndpoint(request.start()) || !validEndpoint(request.end())) {
            return false;
        }
        return request.conditions() == null
                || request.conditions().stream().allMatch(java.util.Objects::nonNull);
    }

    private boolean validEndpoint(RouteEndpoint endpoint) {
        if (endpoint == null || endpoint.type() == null) {
            return false;
        }
        return switch (endpoint.type()) {
            case BUILDING, PLACE -> endpoint.locationId() != null && endpoint.locationId() > 0
                    && endpoint.latitude() == null && endpoint.longitude() == null;
            case COORD -> endpoint.locationId() == null && validLatitude(endpoint.latitude())
                    && validLongitude(endpoint.longitude());
        };
    }

    private boolean validLatitude(Double value) {
        return value != null && Double.isFinite(value) && value >= -90.0 && value <= 90.0;
    }

    private boolean validLongitude(Double value) {
        return value != null && Double.isFinite(value) && value >= -180.0 && value <= 180.0;
    }

    private devkor.com.teamcback.domain.routes.entity.LocationType toDomainType(RouteEndpoint endpoint) {
        return devkor.com.teamcback.domain.routes.entity.LocationType.valueOf(endpoint.type().name());
    }

    private Conditions toDomainCondition(RouteCondition condition) {
        return Conditions.valueOf(condition.name());
    }

    private FindRouteToolData toRouteData(GetRouteRes route) {
        List<RouteStep> steps = route.getPath() == null ? List.of()
                : route.getPath().stream().map(this::toRouteStep).toList();
        return new FindRouteToolData(route.getDuration(), steps);
    }

    private RouteStep toRouteStep(PartialRouteRes step) {
        boolean indoor = step.inOut;
        return new RouteStep(indoor ? RouteSectionType.INDOOR : RouteSectionType.OUTDOOR,
                indoor ? step.buildingId : null, indoor ? step.floor : null, normalizeDetail(step.info));
    }

    private CampusToolError mapRouteError(GlobalException exception) {
        if (exception.getResultCode() == NOT_FOUND_ROUTE || exception.getResultCode() == NOT_FOUND_NODE
                || exception.getResultCode() == NOT_FOUND_BUILDING || exception.getResultCode() == NOT_FOUND_PLACE
                || exception.getResultCode() == COORDINATES_TOO_FAR) {
            return error(NOT_FOUND);
        }
        if (exception.getResultCode() == NOT_PROVIDED_ROUTE
                || exception.getResultCode() == COORDINATES_TOO_NEAR) {
            return error(UNSUPPORTED);
        }
        return error(TEMPORARILY_UNAVAILABLE);
    }

    public GetCafeteriaMenuToolResult getCafeteriaMenu(GetCafeteriaMenuToolRequest request) {
        if (request == null || request.placeId() == null || request.placeId() <= 0
                || request.startDate() == null) {
            return new GetCafeteriaMenuToolResult(null, error(INVALID_INPUT));
        }
        LocalDate endDate = request.endDate() == null ? request.startDate() : request.endDate();
        long inclusiveDays = ChronoUnit.DAYS.between(request.startDate(), endDate) + 1;
        if (inclusiveDays <= 0 || inclusiveDays > properties.tools().menuMaxDays()) {
            return new GetCafeteriaMenuToolResult(null, error(INVALID_INPUT));
        }

        try {
            GetCafeteriaMenuListRes response = cafeteriaMenuService.getCafeteriaMenu(
                    request.placeId(), request.startDate(), endDate.plusDays(1));
            List<CafeteriaMenuDay> days = response.getMenus().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new CafeteriaMenuDay(entry.getKey(), toMeals(entry.getValue())))
                    .toList();
            if (days.stream().allMatch(day -> day.meals().isEmpty())) {
                return new GetCafeteriaMenuToolResult(null, error(NO_DATA));
            }
            return new GetCafeteriaMenuToolResult(
                    new GetCafeteriaMenuToolData(response.getPlaceId(), response.getPlaceName(), days), null);
        } catch (DateTimeException exception) {
            return new GetCafeteriaMenuToolResult(null, error(INVALID_INPUT));
        } catch (GlobalException exception) {
            return new GetCafeteriaMenuToolResult(null, mapDomainError(exception));
        } catch (RuntimeException exception) {
            return new GetCafeteriaMenuToolResult(null, error(TEMPORARILY_UNAVAILABLE));
        }
    }

    public GetRoomCoursesToolResult getRoomCourses(GetRoomCoursesToolRequest request) {
        if (request == null || request.placeId() == null || request.placeId() <= 0) {
            return new GetRoomCoursesToolResult(null, error(INVALID_INPUT));
        }
        try {
            GetCourseListRes response = courseService.getCourseList(request.placeId());
            List<GetCourseRes> allRows = response.getCourses().values().stream().flatMap(List::stream).toList();
            List<GetCourseRes> selectedRows = response.getCourses().entrySet().stream()
                    .filter(entry -> request.weekday() == null || entry.getKey() == request.weekday())
                    .sorted(Map.Entry.comparingByKey())
                    .flatMap(entry -> entry.getValue().stream())
                    .toList();
            if (selectedRows.isEmpty()) {
                return new GetRoomCoursesToolResult(null, error(NO_DATA));
            }
            GetCourseRes metadata = allRows.isEmpty() ? selectedRows.get(0) : allRows.get(0);
            return new GetRoomCoursesToolResult(new GetRoomCoursesToolData(
                    response.getPlaceName(), metadata.getYear(), metadata.getTerm(), mergeCoursePeriods(selectedRows)), null);
        } catch (GlobalException exception) {
            return new GetRoomCoursesToolResult(null, mapDomainError(exception));
        } catch (RuntimeException exception) {
            return new GetRoomCoursesToolResult(null, error(TEMPORARILY_UNAVAILABLE));
        }
    }

    public CampusStatusToolResult getCampusStatus() {
        try {
            return new CampusStatusToolResult(
                    schoolCalendarService.getTerm().getTerm(),
                    schoolCalendarService.isVacation().isActive(),
                    schoolCalendarService.isKoyeon().isActive(),
                    null);
        } catch (RuntimeException exception) {
            return new CampusStatusToolResult(null, false, false, error(TEMPORARILY_UNAVAILABLE));
        }
    }

    private List<CafeteriaMealToolItem> toMeals(Map<String, String> menuByType) {
        if (menuByType == null || menuByType.isEmpty()) {
            return List.of();
        }
        return menuByType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new CafeteriaMealToolItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<RoomCourseToolItem> mergeCoursePeriods(List<GetCourseRes> rows) {
        List<MergedCourse> merged = new ArrayList<>();
        for (GetCourseRes row : rows) {
            Weekday weekday;
            try {
                weekday = Weekday.valueOf(row.getWeekday());
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new IllegalStateException("Invalid course weekday");
            }
            MergedCourse current = MergedCourse.from(row, weekday);
            if (!merged.isEmpty() && merged.get(merged.size() - 1).canMerge(current)) {
                MergedCourse previous = merged.get(merged.size() - 1);
                merged.set(merged.size() - 1, previous.extendTo(current.endPeriod()));
            } else {
                merged.add(current);
            }
        }
        return merged.stream().map(MergedCourse::toToolItem).toList();
    }

    private record MergedCourse(Long courseId, String subject, String professor, String courseCode,
                                String section, Weekday weekday, int startPeriod, int endPeriod) {
        static MergedCourse from(GetCourseRes row, Weekday weekday) {
            return new MergedCourse(row.getCourseId(), row.getSubject(), row.getProfessor(), row.getCode(),
                    row.getSection(), weekday, row.getClassTime(), row.getClassTime());
        }

        boolean canMerge(MergedCourse next) {
            return Objects.equals(courseId, next.courseId)
                    && Objects.equals(subject, next.subject)
                    && Objects.equals(professor, next.professor)
                    && Objects.equals(courseCode, next.courseCode)
                    && Objects.equals(section, next.section)
                    && weekday == next.weekday
                    && next.startPeriod == endPeriod + 1;
        }

        MergedCourse extendTo(int period) {
            return new MergedCourse(courseId, subject, professor, courseCode, section,
                    weekday, startPeriod, period);
        }

        RoomCourseToolItem toToolItem() {
            return new RoomCourseToolItem(subject, professor, courseCode, section,
                    weekday, startPeriod, endPeriod);
        }
    }

    public GetCrowdStatusToolResult getCrowdStatus(GetCrowdStatusToolRequest request) {
        if (request == null || request.placeId() == null || request.placeId() <= 0) {
            return new GetCrowdStatusToolResult(null, error(INVALID_INPUT));
        }
        try {
            GetBLERes response = bleService.getBLE(request.placeId());
            boolean stale = response.getLastStatus() == BLEstatus.FAILURE.getCode();
            List<TypicalCrowdPattern> pattern = Boolean.TRUE.equals(request.includeTypicalPattern())
                    ? toTypicalPattern(bleService.getBLETimePattern(request.placeId())) : null;
            return new GetCrowdStatusToolResult(new CrowdStatusToolData(
                    response.getPlaceId(), response.getLastCount(), response.getCapacity(),
                    toCrowdLevel(response.getLastStatus()), response.getLastTime(), stale, pattern), null);
        } catch (GlobalException exception) {
            return new GetCrowdStatusToolResult(null, mapCrowdError(exception));
        } catch (RuntimeException exception) {
            return new GetCrowdStatusToolResult(null, error(TEMPORARILY_UNAVAILABLE));
        }
    }

    public GetPlaceReviewsToolResult getPlaceReviews(GetPlaceReviewsToolRequest request) {
        if (request == null || request.placeId() == null || request.placeId() <= 0) {
            return new GetPlaceReviewsToolResult(null, error(INVALID_INPUT));
        }
        Integer limit = resolveLimit(request.limit(), properties.tools().reviews());
        if (limit == null) {
            return new GetPlaceReviewsToolResult(null, error(INVALID_INPUT));
        }
        try {
            GetReviewPlaceDetailRes response = reviewService.getReviewPlaceDetail(request.placeId());
            List<SearchPlaceReviewRes> sourceReviews = response.getReviewList();
            if (sourceReviews == null || sourceReviews.isEmpty()) {
                return new GetPlaceReviewsToolResult(null, error(NO_DATA));
            }
            List<ReviewSummary> reviews = sourceReviews.stream().limit(limit)
                    .map(item -> new ReviewSummary(item.getComment(), item.isRevisit(), item.getCreatedAt()))
                    .toList();
            List<ReviewTagSummary> tags = response.getTagList() == null ? List.of()
                    : response.getTagList().stream()
                    .map(item -> new ReviewTagSummary(item.getTag(), item.getNum())).toList();
            return new GetPlaceReviewsToolResult(new PlaceReviewsToolData(
                    response.getPlaceId(), response.getName(), parseRating(response.getStarAverage()), tags,
                    reviews, sourceReviews.size() > limit), null);
        } catch (GlobalException exception) {
            return new GetPlaceReviewsToolResult(null, mapReviewError(exception));
        } catch (RuntimeException exception) {
            return new GetPlaceReviewsToolResult(null, error(TEMPORARILY_UNAVAILABLE));
        }
    }

    private CrowdLevel toCrowdLevel(int status) {
        return switch (status) {
            case 0 -> CrowdLevel.VACANT;
            case 1 -> CrowdLevel.AVAILABLE;
            case 2 -> CrowdLevel.CROWDED;
            default -> CrowdLevel.UNKNOWN;
        };
    }

    private List<TypicalCrowdPattern> toTypicalPattern(BLETimePatternRes pattern) {
        if (pattern == null || pattern.getHours() == null || pattern.getDayOfWeeks() == null
                || pattern.getAverages() == null) {
            return List.of();
        }
        List<TypicalCrowdPattern> result = new ArrayList<>();
        int dayCount = Math.min(pattern.getDayOfWeeks().length, pattern.getAverages().length);
        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            int[] averages = pattern.getAverages()[dayIndex];
            if (averages == null) {
                continue;
            }
            int hourCount = Math.min(pattern.getHours().length, averages.length);
            for (int hourIndex = 0; hourIndex < hourCount; hourIndex++) {
                result.add(new TypicalCrowdPattern(pattern.getDayOfWeeks()[dayIndex],
                        pattern.getHours()[hourIndex], averages[hourIndex]));
            }
        }
        return result;
    }

    private CampusToolError mapCrowdError(GlobalException exception) {
        if (exception.getResultCode() == NOT_FOUND_DEVICE || exception.getResultCode() == NO_DATA_FOR_DEVICE) {
            return error(NO_DATA);
        }
        return mapDomainError(exception);
    }

    private CampusToolError mapReviewError(GlobalException exception) {
        if (exception.getResultCode() == NOT_SUPPORTED_PLACE_TYPE) {
            return error(UNSUPPORTED);
        }
        return mapDomainError(exception);
    }

    private Double parseRating(String rating) {
        if (rating == null) {
            return null;
        }
        try {
            double value = Double.parseDouble(rating);
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean validFacilityRequest(FindFacilitiesToolRequest request) {
        if (request == null) {
            return false;
        }
        if (request.buildingId() != null && request.buildingId() <= 0) {
            return false;
        }
        if (request.floor() != null && request.buildingId() == null) {
            return false;
        }
        if (request.facilityType() == null && request.buildingId() == null) {
            return false;
        }
        return request.facilityType() != null || request.floor() != null;
    }

    private List<FacilityToolItem> flattenBuildingFacilities(FindFacilitiesToolRequest request) {
        Map<Double, List<SearchFacilityRes>> byFloor = searchService.searchBuildingFacilityByType(
                request.buildingId(), request.facilityType().toPlaceType()).getFacilities();
        return byFloor.entrySet().stream()
                .filter(entry -> request.floor() == null
                        || Double.compare(entry.getKey(), request.floor().doubleValue()) == 0)
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .flatMap(entry -> entry.getValue().stream())
                .map(this::toBuildingFacilityItem)
                .toList();
    }

    private SearchCampusItem toSearchItem(ChatbotSearchCandidate item, SearchCampusMatchType matchType) {
        ToolLocationType type = item.locationType() == LocationType.BUILDING
                ? ToolLocationType.BUILDING : ToolLocationType.PLACE;
        Long buildingId = type == ToolLocationType.BUILDING ? item.locationId() : item.buildingId();
        String buildingName = type == ToolLocationType.BUILDING ? item.name() : null;
        return new SearchCampusItem(item.locationId(), type, item.name(), buildingId, buildingName,
                item.floor(), item.placeType(), normalizeDetail(item.detail()), matchType);
    }

    private SearchCampusMatchType matchType(String normalizedQuery, String candidateName) {
        String normalizedCandidate = normalizeSearchName(candidateName);
        if (normalizedQuery.equals(normalizedCandidate)) {
            return SearchCampusMatchType.EXACT;
        }
        if (!normalizedCandidate.isEmpty() && normalizedQuery.contains(normalizedCandidate)) {
            return SearchCampusMatchType.STRONG;
        }
        return SearchCampusMatchType.PARTIAL;
    }

    private long elapsedMillis(long startedAt) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private record RankedSearchCandidate(ChatbotSearchCandidate candidate, SearchCampusMatchType matchType) {
    }

    private String normalizeSearchName(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFC)
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private LocationDetailToolData toBuildingDetail(SearchBuildingDetailRes detail) {
        return new LocationDetailToolData(detail.getBuildingId(), ToolLocationType.BUILDING,
                detail.getName(), detail.getBuildingId(), detail.getName(), null, detail.getAddress(),
                normalizeDetail(detail.getDetails()), null, detail.isOperating(), detail.getNextBuildingTime(),
                detail.getWeekdayOperatingTime(), detail.getSaturdayOperatingTime(),
                detail.getSundayOperatingTime(), null, null, null, null);
    }

    private LocationDetailToolData toPlaceDetail(SearchPlaceDetailRes detail) {
        return new LocationDetailToolData(detail.getPlaceId(), ToolLocationType.PLACE, detail.getName(),
                detail.getBuildingId(), null, detail.getFloor(), null, normalizeDetail(detail.getDetail()),
                detail.getPlaceType(), detail.isOperating(), detail.getNextPlaceTime(),
                detail.getWeekdayOperatingTime(), detail.getSaturdayOperatingTime(),
                detail.getSundayOperatingTime(), detail.isAvailability(), detail.isPlugAvailability(),
                null, parseMeaningfulRating(detail.getStarAverage()));
    }

    private FacilityToolItem toFacilityItem(SearchPlaceRes item) {
        return new FacilityToolItem(item.getId(), item.getName(), item.getPlaceType(), item.getBuildingId(),
                item.getBuildingName(), toInteger(item.getFloor()), normalizeDetail(item.getDetail()),
                item.isOperating(), null, item.getPlugAvailability());
    }

    private FacilityToolItem toBuildingFacilityItem(SearchFacilityRes item) {
        return new FacilityToolItem(item.getId(), item.getName(), item.getPlaceType(), item.getBuildingId(),
                null, item.getFloor(), normalizeDetail(item.getDetail()), item.isOperating(),
                item.getAvailability(), null);
    }

    private FacilityToolItem toFloorItem(SearchRoomDetailRes item, Long buildingId, Integer floor) {
        return new FacilityToolItem(item.getId(), item.getName(), item.getPlaceType(), buildingId, null, floor,
                normalizeDetail(item.getDetail()), item.isOperating(), item.isAvailability(),
                item.isPlugAvailability());
    }

    private Integer resolveLimit(Integer requested, ChatbotProperties.Limits limits) {
        if (requested != null && requested <= 0) {
            return null;
        }
        int value = requested == null ? limits.defaultLimit() : requested;
        return Math.min(value, limits.maxLimit());
    }

    private CampusToolError mapDomainError(GlobalException exception) {
        if (exception.getResultCode() == NOT_FOUND_BUILDING || exception.getResultCode() == NOT_FOUND_PLACE) {
            return error(NOT_FOUND);
        }
        return error(TEMPORARILY_UNAVAILABLE);
    }

    private CampusToolError error(CampusToolErrorCode code) {
        return CampusToolError.of(code);
    }

    private String normalizeDetail(String detail) {
        return detail == null || detail.isBlank() || ".".equals(detail) ? null : detail;
    }

    private Integer toInteger(Double floor) {
        return floor == null ? null : floor.intValue();
    }

    private Double parseMeaningfulRating(String rating) {
        if (rating == null) {
            return null;
        }
        try {
            double value = Double.parseDouble(rating);
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
