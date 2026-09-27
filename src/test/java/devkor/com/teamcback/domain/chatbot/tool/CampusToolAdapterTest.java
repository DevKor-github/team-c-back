package devkor.com.teamcback.domain.chatbot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.search.ChatbotCampusSearchService;
import devkor.com.teamcback.domain.chatbot.search.ChatbotSearchCandidate;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import devkor.com.teamcback.domain.routes.service.RouteService;
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
    private ChatbotCampusSearchService chatbotCampusSearchService;
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
        adapter = new CampusToolAdapter(searchService, chatbotCampusSearchService, routeService,
                null, null, null, null, null, properties);
    }

    @Test
    void keepsLightweightResolverOrderAndAmbiguity() {
        when(chatbotCampusSearchService.search("중도")).thenReturn(List.of(
                candidate(11L, "중앙도서관(대학원)", ToolLocationType.BUILDING),
                candidate(12L, "중앙도서관(신관)", ToolLocationType.BUILDING)));

        var result = adapter.searchCampus(new SearchCampusToolRequest(" 중도 ", 1));

        assertThat(result.ambiguous()).isTrue();
        assertThat(result.candidates()).extracting("locationId").containsExactly(11L, 12L);
        verify(chatbotCampusSearchService).search("중도");
        verifyNoInteractions(searchService);
    }

    @Test
    void preservesResolverCandidateOrder() {
        when(chatbotCampusSearchService.search("문과대학 서관")).thenReturn(List.of(
                candidate(21L, "문과대학(서관)", ToolLocationType.BUILDING),
                candidate(29L, "문과대학(서관) 129B", ToolLocationType.PLACE)));

        var result = adapter.searchCampus(new SearchCampusToolRequest("문과대학 서관", null));

        assertThat(result.candidates()).extracting("locationId").containsExactly(21L, 29L);
        assertThat(result.ambiguous()).isTrue();
    }

    @Test
    void keepsOnlyResolverLocations() {
        when(chatbotCampusSearchService.search("시설")).thenReturn(List.of(
                candidate(31L, "중앙도서관 장애인주차장", ToolLocationType.PLACE),
                candidate(32L, "중앙도서관 129B", ToolLocationType.PLACE)));

        var result = adapter.searchCampus(new SearchCampusToolRequest("시설", 1));

        assertThat(result.candidates()).extracting("locationId").containsExactly(31L, 32L);
        assertThat(result.candidates()).allMatch(item -> item.locationId() != null);
    }

    @Test
    void resolverDedupeIsPreserved() {
        when(chatbotCampusSearchService.search("중도")).thenReturn(List.of(
                candidate(11L, "중앙도서관", ToolLocationType.BUILDING),
                candidate(12L, "중앙도서관(신관)", ToolLocationType.BUILDING)));

        var result = adapter.searchCampus(new SearchCampusToolRequest("중도", null));

        assertThat(result.candidates()).extracting("locationId").containsExactly(11L, 12L);
        assertThat(result.ambiguous()).isTrue();
    }

    @Test
    void keepsAtMostServerSearchLimitAfterLightweightSearch() {
        List<ChatbotSearchCandidate> results = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(id -> candidate(id, "장소" + id, ToolLocationType.BUILDING))
                .toList();
        when(chatbotCampusSearchService.search("장소")).thenReturn(results);

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
        when(chatbotCampusSearchService.search("없는 곳")).thenReturn(List.of());

        var result = adapter.searchCampus(new SearchCampusToolRequest("없는 곳", null));

        assertThat(result.error().code()).isEqualTo(devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.NOT_FOUND);
    }

    private ChatbotSearchCandidate candidate(long id, String name, ToolLocationType type) {
        return new ChatbotSearchCandidate(id, type, name, type == ToolLocationType.PLACE ? 1L : id,
                type == ToolLocationType.PLACE ? "건물" : name,
                type == ToolLocationType.PLACE ? 1.0 : null,
                type == ToolLocationType.PLACE ? PlaceType.CLASSROOM : null, null, 100);
    }
}
