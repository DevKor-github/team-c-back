package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusIntent;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusRole;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class ResolvedLocationCollectorTest {
    @Test
    void recordsOnlyAUniqueNonAmbiguousToolResult() {
        ResolvedLocationCollector collector = new ResolvedLocationCollector();
        SearchCampusToolRequest request = new SearchCampusToolRequest(
                "중앙도서관", 5, SearchCampusRole.END, SearchCampusIntent.NAVIGATE_ROUTE,
                List.of(RouteCondition.BARRIERFREE));
        collector.record(request, new SearchCampusToolResult(List.of(
                new SearchCampusItem(456L, ToolLocationType.BUILDING, "중앙도서관", 456L,
                        "중앙도서관", null, null, null, null)), false, null));

        assertThat(collector.snapshot()).singleElement().satisfies(location -> {
            assertThat(location.id()).isEqualTo(456L);
            assertThat(location.role().name()).isEqualTo("END");
            assertThat(location.intent().name()).isEqualTo("NAVIGATE_ROUTE");
        });
    }

    @Test
    void ignoresAmbiguousResultsAndDeduplicatesSameReference() {
        ResolvedLocationCollector collector = new ResolvedLocationCollector();
        SearchCampusToolRequest request = new SearchCampusToolRequest(
                "학생회관", 5, SearchCampusRole.START, SearchCampusIntent.NAVIGATE_ROUTE, List.of());
        SearchCampusItem item = new SearchCampusItem(123L, ToolLocationType.BUILDING, "학생회관", 123L,
                "학생회관", null, null, null, null);
        collector.record(request, new SearchCampusToolResult(List.of(item), true, null));
        collector.record(request, new SearchCampusToolResult(List.of(item), false, null));
        collector.record(request, new SearchCampusToolResult(List.of(item), false, null));

        assertThat(collector.snapshot()).hasSize(1);
        assertThat(collector.searchResolutionSnapshot()).hasSize(3);
        assertThat(collector.searchResolutionSnapshot().get(0).ambiguous()).isTrue();
        assertThat(collector.searchResolutionSnapshot().get(0).candidates()).singleElement()
                .satisfies(candidate -> assertThat(candidate.locationId()).isEqualTo(123L));
    }

    @Test
    void separateCollectorsDoNotShareResolvedLocations() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<List<devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation>> first = executor.submit(() -> {
                ResolvedLocationCollector collector = new ResolvedLocationCollector();
                collector.record(new SearchCampusToolRequest("one", 5, SearchCampusRole.START,
                        SearchCampusIntent.NAVIGATE_ROUTE, List.of()), uniqueResult(1L, "one"));
                return collector.snapshot();
            });
            Future<List<devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation>> second = executor.submit(() -> {
                ResolvedLocationCollector collector = new ResolvedLocationCollector();
                collector.record(new SearchCampusToolRequest("two", 5, SearchCampusRole.END,
                        SearchCampusIntent.NAVIGATE_ROUTE, List.of()), uniqueResult(2L, "two"));
                return collector.snapshot();
            });
            assertThat(first.get()).extracting("id").containsExactly(1L);
            assertThat(second.get()).extracting("id").containsExactly(2L);
        } finally {
            executor.shutdownNow();
        }
    }

    private SearchCampusToolResult uniqueResult(Long id, String name) {
        return new SearchCampusToolResult(List.of(new SearchCampusItem(id, ToolLocationType.BUILDING,
                name, id, name, null, null, null, null)), false, null);
    }
}
