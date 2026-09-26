package devkor.com.teamcback.domain.chatbot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.routes.service.RouteService;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchListRes;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchRes;
import devkor.com.teamcback.domain.search.service.SearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampusToolAdapterTest {
    @Mock
    private SearchService searchService;
    @Mock
    private RouteService routeService;

    private CampusToolAdapter adapter;

    @BeforeEach
    void setUp() {
        ChatbotProperties properties = new ChatbotProperties(true,
                new ChatbotProperties.Llm("google", "gemini-2.5-flash-lite", 500, 8),
                new ChatbotProperties.Agent(6, 5, 60, 15),
                new ChatbotProperties.Tools(
                        new ChatbotProperties.Limits(5, 10),
                        new ChatbotProperties.Limits(10, 20), 7,
                        new ChatbotProperties.Limits(5, 10)),
                new ChatbotProperties.RateLimit(30, 10, 5, "Asia/Seoul"));
        adapter = new CampusToolAdapter(searchService, routeService, null, null, null, null, null, properties);
    }

    @Test
    void reusesGlobalSearchOrderAndPassesAnonymousUser() {
        doReturn(new GlobalSearchListRes(List.of(
                searchResult(11L, "중앙도서관(대학원)", LocationType.BUILDING, null),
                searchResult(12L, "중앙도서관(신관)", LocationType.BUILDING, null))))
                .when(searchService).globalSearch("중도", null);

        var result = adapter.searchCampus(new SearchCampusToolRequest(" 중도 ", 1));

        assertThat(result.ambiguous()).isTrue();
        assertThat(result.candidates()).extracting("locationId").containsExactly(11L, 12L);
        verify(searchService).globalSearch("중도", null);
    }

    @Test
    void doesNotReRankGlobalSearchResultsByMatchType() {
        doReturn(new GlobalSearchListRes(List.of(
                searchResult(21L, "문과대학(서관)", LocationType.BUILDING, null),
                searchResult(29L, "문과대학(서관) 129B", LocationType.PLACE, PlaceType.CLASSROOM))))
                .when(searchService).globalSearch("문과대학 서관", null);

        var result = adapter.searchCampus(new SearchCampusToolRequest("문과대학 서관", null));

        assertThat(result.candidates()).extracting("locationId").containsExactly(21L, 29L);
        assertThat(result.ambiguous()).isTrue();
    }

    @Test
    void filtersVirtualFacilitiesAndInvalidIdsWhileKeepingGlobalOrder() {
        doReturn(new GlobalSearchListRes(List.of(
                searchResult(null, "장애인주차장", LocationType.FACILITY, PlaceType.DISABLED_PARKING),
                searchResult(31L, "중앙도서관 장애인주차장", LocationType.PLACE, PlaceType.DISABLED_PARKING),
                searchResult(32L, "중앙도서관 129B", LocationType.PLACE, PlaceType.CLASSROOM))))
                .when(searchService).globalSearch("시설", null);

        var result = adapter.searchCampus(new SearchCampusToolRequest("시설", 1));

        assertThat(result.candidates()).extracting("locationId").containsExactly(31L, 32L);
        assertThat(result.candidates()).allMatch(item -> item.locationId() != null);
    }

    @Test
    void deduplicatesSameLocationWithoutMergingDifferentIds() {
        GlobalSearchRes first = searchResult(11L, "중앙도서관", LocationType.BUILDING, null);
        GlobalSearchRes duplicate = searchResult(11L, "중앙도서관", LocationType.BUILDING, null);
        GlobalSearchRes different = searchResult(12L, "중앙도서관(신관)", LocationType.BUILDING, null);
        doReturn(new GlobalSearchListRes(List.of(first, duplicate, different)))
                .when(searchService).globalSearch("중도", null);

        var result = adapter.searchCampus(new SearchCampusToolRequest("중도", null));

        assertThat(result.candidates()).extracting("locationId").containsExactly(11L, 12L);
        assertThat(result.ambiguous()).isTrue();
    }

    @Test
    void keepsAtMostServerSearchLimitAfterGlobalRanking() {
        List<GlobalSearchRes> results = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(id -> searchResult((long) id, "장소" + id, LocationType.BUILDING, null))
                .toList();
        doReturn(new GlobalSearchListRes(results)).when(searchService).globalSearch("장소", null);

        var result = adapter.searchCampus(new SearchCampusToolRequest("장소", 1));

        assertThat(result.candidates()).hasSize(5);
        assertThat(result.ambiguous()).isTrue();
    }

    @Test
    void blankSearchDoesNotCallSearchService() {
        var result = adapter.searchCampus(new SearchCampusToolRequest("   ", null));

        assertThat(result.error().code()).isEqualTo(devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.INVALID_INPUT);
        verifyNoInteractions(searchService);
    }

    @Test
    void emptyGlobalSearchMapsToNotFound() {
        doReturn(new GlobalSearchListRes(List.of())).when(searchService).globalSearch("없는 곳", null);

        var result = adapter.searchCampus(new SearchCampusToolRequest("없는 곳", null));

        assertThat(result.error().code()).isEqualTo(devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.NOT_FOUND);
    }

    private GlobalSearchRes searchResult(Long id, String name, LocationType type, PlaceType placeType) {
        GlobalSearchRes result = mock(GlobalSearchRes.class);
        lenient().when(result.getId()).thenReturn(id);
        lenient().when(result.getName()).thenReturn(name);
        lenient().when(result.getLocationType()).thenReturn(type);
        lenient().when(result.getPlaceType()).thenReturn(placeType);
        lenient().when(result.getBuildingId()).thenReturn(type == LocationType.PLACE ? 1L : null);
        lenient().when(result.getFloor()).thenReturn(type == LocationType.PLACE ? 1.0 : null);
        lenient().when(result.getDetail()).thenReturn(null);
        return result;
    }
}
