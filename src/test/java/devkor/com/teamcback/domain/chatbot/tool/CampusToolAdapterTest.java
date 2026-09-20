package devkor.com.teamcback.domain.chatbot.tool;

import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusFacilityType.TOILET;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.AMBIGUOUS_LOCATION;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.INVALID_INPUT;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.NOT_FOUND;
import static devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType.BUILDING;
import static devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType.PLACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.tool.dto.FacilityToolItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindFacilitiesToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchListRes;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchRes;
import devkor.com.teamcback.domain.search.dto.response.SearchBuildingDetailRes;
import devkor.com.teamcback.domain.search.dto.response.SearchBuildingFacilityListRes;
import devkor.com.teamcback.domain.search.dto.response.SearchFacilityListRes;
import devkor.com.teamcback.domain.search.dto.response.SearchFacilityRes;
import devkor.com.teamcback.domain.search.dto.response.SearchFloorInfoRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceDetailRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceRes;
import devkor.com.teamcback.domain.search.dto.response.SearchRoomDetailRes;
import devkor.com.teamcback.domain.search.service.SearchService;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampusToolAdapterTest {
    @Mock
    private SearchService searchService;

    private CampusToolAdapter adapter;

    @BeforeEach
    void setUp() {
        ChatbotProperties properties = new ChatbotProperties(true,
                new ChatbotProperties.Llm("google", "gemini-2.5-flash-lite", 500, 8),
                new ChatbotProperties.Agent(6, 5, 60),
                new ChatbotProperties.Tools(
                        new ChatbotProperties.Limits(5, 10),
                        new ChatbotProperties.Limits(10, 20)));
        adapter = new CampusToolAdapter(searchService, properties);
    }

    @Test
    void centralLibraryNicknameSearchReturnsBuildingCandidateWithoutPersonalization() {
        GlobalSearchRes building = searchResult(11L, "중앙도서관", LocationType.BUILDING, null);
        when(searchService.globalSearch("중도", null)).thenReturn(new GlobalSearchListRes(List.of(building)));

        var result = adapter.searchCampus(new SearchCampusToolRequest("  중도  ", null));

        assertThat(result.error()).isNull();
        assertThat(result.ambiguous()).isFalse();
        assertThat(result.candidates()).singleElement().satisfies(item -> {
            assertThat(item.locationId()).isEqualTo(11L);
            assertThat(item.locationType()).isEqualTo(BUILDING);
            assertThat(item.name()).isEqualTo("중앙도서관");
        });
        verify(searchService).globalSearch("중도", null);
    }

    @Test
    void multipleSearchCandidatesRemainAmbiguousInsteadOfBeingSelected() {
        GlobalSearchRes first = searchResult(11L, "중앙도서관", LocationType.BUILDING, null);
        GlobalSearchRes second = searchResult(12L, "중앙도서관 신관", LocationType.BUILDING, null);
        when(searchService.globalSearch("중앙도서관", null))
                .thenReturn(new GlobalSearchListRes(List.of(first, second)));

        var result = adapter.searchCampus(new SearchCampusToolRequest("중앙도서관", 1));

        assertThat(result.ambiguous()).isTrue();
        assertThat(result.error().code()).isEqualTo(AMBIGUOUS_LOCATION);
        assertThat(result.candidates()).extracting("locationId").containsExactly(11L);
    }

    @Test
    void searchFiltersVirtualFacilityAndEnforcesMaximumLimit() {
        GlobalSearchRes virtualFacility = searchResult(null, "화장실", LocationType.FACILITY, PlaceType.TOILET);
        List<GlobalSearchRes> places = IntStream.rangeClosed(1, 12)
                .mapToObj(id -> searchResult((long) id, "장소" + id, LocationType.PLACE, PlaceType.CAFE))
                .toList();
        List<GlobalSearchRes> mixed = new java.util.ArrayList<>();
        mixed.add(virtualFacility);
        mixed.addAll(places);
        when(searchService.globalSearch("시설", null)).thenReturn(new GlobalSearchListRes(mixed));

        var result = adapter.searchCampus(new SearchCampusToolRequest("시설", 100));

        assertThat(result.candidates()).hasSize(10)
                .allMatch(item -> item.locationId() != null && item.locationType() == PLACE);
    }

    @Test
    void blankSearchAndNoResultsReturnStableErrors() {
        assertThat(adapter.searchCampus(new SearchCampusToolRequest("   ", null)).error().code())
                .isEqualTo(INVALID_INPUT);
        verifyNoInteractions(searchService);

        when(searchService.globalSearch("없는 곳", null)).thenReturn(new GlobalSearchListRes(List.of()));
        assertThat(adapter.searchCampus(new SearchCampusToolRequest("없는 곳", null)).error().code())
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void centralLibraryOpenQuestionUsesBuildingDetailPath() {
        SearchBuildingDetailRes detail = mock(SearchBuildingDetailRes.class);
        when(detail.getBuildingId()).thenReturn(11L);
        when(detail.getName()).thenReturn("고려대학교 중앙도서관");
        when(detail.isOperating()).thenReturn(true);
        when(detail.getNextBuildingTime()).thenReturn("22:00");
        when(detail.getWeekdayOperatingTime()).thenReturn("09:00-22:00");
        when(searchService.searchBuildingDetail(null, 11L)).thenReturn(detail);

        var result = adapter.getLocationDetail(new GetLocationDetailToolRequest(BUILDING, 11L));

        assertThat(result.error()).isNull();
        assertThat(result.location().openNow()).isTrue();
        assertThat(result.location().nextStatusChangeTime()).isEqualTo("22:00");
        verify(searchService).searchBuildingDetail(null, 11L);
        verify(searchService, never()).searchPlaceDetail(null, 11L);
    }

    @Test
    void placeDetailMapsOperatingAndPlugFields() {
        SearchPlaceDetailRes detail = mock(SearchPlaceDetailRes.class);
        when(detail.getPlaceId()).thenReturn(31L);
        when(detail.getBuildingId()).thenReturn(3L);
        when(detail.getName()).thenReturn("하나스퀘어 카페");
        when(detail.getPlaceType()).thenReturn(PlaceType.CAFE);
        when(detail.isOperating()).thenReturn(false);
        when(detail.getNextPlaceTime()).thenReturn("09:00");
        when(detail.isAvailability()).thenReturn(true);
        when(detail.isPlugAvailability()).thenReturn(true);
        when(detail.getStarAverage()).thenReturn("4.25");
        when(searchService.searchPlaceDetail(null, 31L)).thenReturn(detail);

        var result = adapter.getLocationDetail(new GetLocationDetailToolRequest(PLACE, 31L));

        assertThat(result.location().openNow()).isFalse();
        assertThat(result.location().available()).isTrue();
        assertThat(result.location().plugAvailable()).isTrue();
        assertThat(result.location().rating()).isEqualTo(4.25);
        verify(searchService).searchPlaceDetail(null, 31L);
    }

    @Test
    void toolDetailOutputHasNoPrivateOrInternalFields() {
        assertThat(recordFieldNames(devkor.com.teamcback.domain.chatbot.tool.dto.LocationDetailToolData.class))
                .doesNotContain("imageUrl", "longitude", "latitude", "xCoord", "yCoord", "maskIndex",
                        "bookmarked", "userId", "categoryColor", "nodeId");
        assertThat(recordFieldNames(devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusItem.class))
                .doesNotContain("imageUrl", "longitude", "latitude", "bookmarked", "userId", "categoryColor");
    }

    @Test
    void studentCenterToiletUsesCampusWideTypeQuery() {
        SearchPlaceRes toilet = mock(SearchPlaceRes.class);
        when(toilet.getId()).thenReturn(21L);
        when(toilet.getName()).thenReturn("학생회관 1층 화장실");
        when(toilet.getPlaceType()).thenReturn(PlaceType.TOILET);
        when(toilet.getBuildingId()).thenReturn(5L);
        when(toilet.getBuildingName()).thenReturn("학생회관");
        when(searchService.searchFacilitiesWithType(PlaceType.TOILET))
                .thenReturn(new SearchFacilityListRes(List.of(toilet)));

        var result = adapter.findFacilities(new FindFacilitiesToolRequest(TOILET, null, null, null));

        assertThat(result.error()).isNull();
        assertThat(result.facilities()).singleElement().extracting(FacilityToolItem::buildingName)
                .isEqualTo("학생회관");
        verify(searchService).searchFacilitiesWithType(PlaceType.TOILET);
    }

    @Test
    void buildingTypeQueryUsesBuildingFacilityService() {
        SearchFacilityRes toilet = mock(SearchFacilityRes.class);
        when(toilet.getId()).thenReturn(22L);
        when(toilet.getPlaceType()).thenReturn(PlaceType.TOILET);
        when(toilet.getBuildingId()).thenReturn(5L);
        SearchBuildingFacilityListRes response = mock(SearchBuildingFacilityListRes.class);
        when(response.getFacilities()).thenReturn(Map.of(1.0, List.of(toilet)));
        when(searchService.searchBuildingFacilityByType(5L, PlaceType.TOILET)).thenReturn(response);

        var result = adapter.findFacilities(new FindFacilitiesToolRequest(TOILET, 5L, null, null));

        assertThat(result.facilities()).singleElement().extracting(FacilityToolItem::placeId).isEqualTo(22L);
        verify(searchService).searchBuildingFacilityByType(5L, PlaceType.TOILET);
    }

    @Test
    void buildingFloorQueryExcludesNavigationNodes() {
        SearchRoomDetailRes room = mock(SearchRoomDetailRes.class);
        when(room.getId()).thenReturn(41L);
        when(room.getName()).thenReturn("101호");
        when(room.getPlaceType()).thenReturn(PlaceType.CLASSROOM);
        SearchFloorInfoRes response = mock(SearchFloorInfoRes.class);
        when(response.getRoomList()).thenReturn(List.of(room));
        when(searchService.searchPlaceByBuildingFloor(5L, 1)).thenReturn(response);

        var result = adapter.findFacilities(new FindFacilitiesToolRequest(null, 5L, 1, null));

        assertThat(result.facilities()).singleElement().satisfies(item -> {
            assertThat(item.placeId()).isEqualTo(41L);
            assertThat(item.floor()).isEqualTo(1);
        });
        verify(searchService).searchPlaceByBuildingFloor(5L, 1);
    }

    @Test
    void floorWithoutBuildingIsRejectedAndFacilityMaximumLimitIsEnforced() {
        var invalid = adapter.findFacilities(new FindFacilitiesToolRequest(null, null, 1, null));
        assertThat(invalid.error().code()).isEqualTo(INVALID_INPUT);
        verifyNoInteractions(searchService);

        List<SearchPlaceRes> facilities = IntStream.range(0, 25).mapToObj(index -> mock(SearchPlaceRes.class)).toList();
        when(searchService.searchFacilitiesWithType(PlaceType.TOILET))
                .thenReturn(new SearchFacilityListRes(facilities));
        var limited = adapter.findFacilities(new FindFacilitiesToolRequest(TOILET, null, null, 100));
        assertThat(limited.facilities()).hasSize(20);
    }

    private GlobalSearchRes searchResult(Long id, String name, LocationType type, PlaceType placeType) {
        GlobalSearchRes result = mock(GlobalSearchRes.class);
        lenient().when(result.getId()).thenReturn(id);
        lenient().when(result.getName()).thenReturn(name);
        lenient().when(result.getLocationType()).thenReturn(type);
        lenient().when(result.getPlaceType()).thenReturn(placeType);
        return result;
    }

    private List<String> recordFieldNames(Class<?> type) {
        return IntStream.range(0, type.getRecordComponents().length)
                .mapToObj(index -> type.getRecordComponents()[index])
                .map(RecordComponent::getName)
                .toList();
    }
}
