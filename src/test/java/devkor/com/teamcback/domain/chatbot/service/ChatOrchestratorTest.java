package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.dto.RoutePlan;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusIntent;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusMatchType;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusRole;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatOrchestratorTest {
    @Mock LlmGateway llmGateway;
    @Mock CampusToolAdapter campusToolAdapter;
    @Mock RoutePlanner routePlanner;

    @Test
    void deterministicTextRouteResolvesBothEndpointsAndExecutesRouteWithoutModelTools() {
        when(routePlanner.plan(anyString(), anyList(), anyString())).thenReturn(
                new RoutePlan(RoutePlan.Intent.TEXT_ROUTE, "start place", "end place", List.of()));
        when(campusToolAdapter.searchCampus(any(SearchCampusToolRequest.class))).thenAnswer(invocation -> {
            SearchCampusToolRequest request = invocation.getArgument(0);
            long id = request.role() == SearchCampusRole.START ? 11L : 22L;
            return resolved(id, request.query());
        });
        when(campusToolAdapter.findRoute(any())).thenReturn(successfulRoute(120L));
        ChatOrchestrator orchestrator = new ChatOrchestrator(llmGateway, campusToolAdapter, routePlanner);

        ChatOrchestrationResult result = orchestrator.execute("system", List.of(), "route");

        assertThat(result.routeExecution()).isNotNull();
        assertThat(result.reply()).contains("start place", "end place");
        verify(campusToolAdapter, org.mockito.Mockito.times(2)).searchCampus(any());
        verify(campusToolAdapter).findRoute(any());
        verify(llmGateway, never()).generate(anyString(), anyList(), anyString(), any());
    }

    @Test
    void formatsDurationAsApproximateMinutes() {
        when(routePlanner.plan(anyString(), anyList(), anyString())).thenReturn(
                new RoutePlan(RoutePlan.Intent.TEXT_ROUTE, "start", "end", List.of()));
        when(campusToolAdapter.searchCampus(any(SearchCampusToolRequest.class))).thenAnswer(invocation -> {
            SearchCampusToolRequest request = invocation.getArgument(0);
            return resolved(request.role() == SearchCampusRole.START ? 11L : 22L, request.query());
        });
        when(campusToolAdapter.findRoute(any())).thenReturn(successfulRoute(421L));

        ChatOrchestrationResult result = new ChatOrchestrator(llmGateway, campusToolAdapter, routePlanner)
                .execute("system", List.of(), "route");

        assertThat(result.reply()).isEqualTo("start에서 end까지 경로를 찾았어요. 예상 소요 시간은 약 7분이에요. 길찾기 화면으로 안내할까요?");
    }

    @Test
    void formatsSubMinuteDurationNaturally() {
        when(routePlanner.plan(anyString(), anyList(), anyString())).thenReturn(
                new RoutePlan(RoutePlan.Intent.TEXT_ROUTE, "start", "end", List.of()));
        when(campusToolAdapter.searchCampus(any(SearchCampusToolRequest.class))).thenAnswer(invocation -> {
            SearchCampusToolRequest request = invocation.getArgument(0);
            return resolved(request.role() == SearchCampusRole.START ? 11L : 22L, request.query());
        });
        when(campusToolAdapter.findRoute(any())).thenReturn(successfulRoute(45L));

        ChatOrchestrationResult result = new ChatOrchestrator(llmGateway, campusToolAdapter, routePlanner)
                .execute("system", List.of(), "route");

        assertThat(result.reply()).isEqualTo("start에서 end까지 경로를 찾았어요. 예상 소요 시간은 1분 이내예요. 길찾기 화면으로 안내할까요?");
    }

    @Test
    void deterministicNavigateRouteResolvesBothEndpointsWithoutFindRoute() {
        when(routePlanner.plan(anyString(), anyList(), anyString())).thenReturn(
                new RoutePlan(RoutePlan.Intent.NAVIGATE_ROUTE, "start place", "end place", List.of()));
        when(campusToolAdapter.searchCampus(any(SearchCampusToolRequest.class))).thenAnswer(invocation -> {
            SearchCampusToolRequest request = invocation.getArgument(0);
            long id = request.role() == SearchCampusRole.START ? 11L : 22L;
            return resolved(id, request.query());
        });
        ChatOrchestrator orchestrator = new ChatOrchestrator(llmGateway, campusToolAdapter, routePlanner);

        ChatOrchestrationResult result = orchestrator.execute("system", List.of(), "route");

        assertThat(result.resolvedLocations()).hasSize(2);
        assertThat(result.routeExecution()).isNull();
        verify(campusToolAdapter, org.mockito.Mockito.times(2)).searchCampus(any());
        verify(campusToolAdapter, never()).findRoute(any());
        verify(llmGateway, never()).generate(anyString(), anyList(), anyString(), any());
    }

    @Test
    void deterministicRouteStopsAfterAmbiguousStart() {
        when(routePlanner.plan(anyString(), anyList(), anyString())).thenReturn(
                new RoutePlan(RoutePlan.Intent.TEXT_ROUTE, "ambiguous", "end place", List.of()));
        when(campusToolAdapter.searchCampus(any(SearchCampusToolRequest.class))).thenReturn(
                new SearchCampusToolResult(List.of(item(11L, "start A"), item(12L, "start B")), true, null));
        ChatOrchestrator orchestrator = new ChatOrchestrator(llmGateway, campusToolAdapter, routePlanner);

        ChatOrchestrationResult result = orchestrator.execute("system", List.of(), "route");

        assertThat(result.routeExecution()).isNull();
        assertThat(result.searchResolutions()).hasSize(1);
        verify(campusToolAdapter).searchCampus(any());
        verify(campusToolAdapter, never()).findRoute(any());
    }

    @Test
    void keepsModelReplyAndDoesNotDuplicateExistingRouteExecution() {
        when(llmGateway.generate(anyString(), anyList(), anyString(), any())).thenAnswer(invocation -> {
            ResolvedLocationCollector state = invocation.getArgument(3);
            recordResolvedRoute(state, SearchCampusIntent.TEXT_ROUTE);
            FindRouteToolRequest request = state.currentTextRouteRequest();
            state.recordRouteExecution(request, successfulRoute(90L));
            return new LlmGateway.LlmResult("모델 경로 답변");
        });
        ChatOrchestrator orchestrator = new ChatOrchestrator(llmGateway, campusToolAdapter);

        ChatOrchestrationResult result = orchestrator.execute("system", List.of(), "route");

        assertThat(result.reply()).isEqualTo("모델 경로 답변");
        assertThat(result.routeExecution()).isNotNull();
        verify(campusToolAdapter, never()).findRoute(any());
    }

    @Test
    void executesMissingTextRouteOnceAfterBothEndpointsResolve() {
        when(llmGateway.generate(anyString(), anyList(), anyString(), any())).thenAnswer(invocation -> {
            recordResolvedRoute(invocation.getArgument(3), SearchCampusIntent.TEXT_ROUTE);
            return new LlmGateway.LlmResult("모델 답변");
        });
        when(campusToolAdapter.findRoute(any())).thenReturn(successfulRoute(120L));
        ChatOrchestrator orchestrator = new ChatOrchestrator(llmGateway, campusToolAdapter);

        ChatOrchestrationResult result = orchestrator.execute("system", List.of(), "route");

        assertThat(result.reply()).isEqualTo("모델 답변");
        assertThat(result.routeExecution()).isNotNull();
        assertThat(result.routeExecution().successful()).isTrue();
        verify(campusToolAdapter).findRoute(any());
    }

    @Test
    void returnsDeterministicReplyFromRouteTraceWhenCompletionIsBlank() {
        when(llmGateway.generate(anyString(), anyList(), anyString(), any())).thenAnswer(invocation -> {
            recordResolvedRoute(invocation.getArgument(3), SearchCampusIntent.TEXT_ROUTE);
            return new LlmGateway.LlmResult(null, LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION);
        });
        when(campusToolAdapter.findRoute(any())).thenReturn(successfulRoute(120L));
        ChatOrchestrator orchestrator = new ChatOrchestrator(llmGateway, campusToolAdapter);

        ChatOrchestrationResult result = orchestrator.execute("system", List.of(), "route");

        assertThat(result.reply()).isEqualTo("start에서 end까지 경로를 찾았어요. 예상 소요 시간은 약 2분이에요. 길찾기 화면으로 안내할까요?");
        assertThat(result.completionStatus()).isEqualTo(LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION);
    }

    @Test
    void doesNotExecuteRouteForAmbiguousTextEndpoint() {
        when(llmGateway.generate(anyString(), anyList(), anyString(), any())).thenAnswer(invocation -> {
            ResolvedLocationCollector state = invocation.getArgument(3);
            state.record(search("start", SearchCampusRole.START, SearchCampusIntent.TEXT_ROUTE), resolved(11L, "start"));
            state.record(search("end", SearchCampusRole.END, SearchCampusIntent.TEXT_ROUTE),
                    new SearchCampusToolResult(List.of(item(22L, "end A"), item(23L, "end B")), true, null));
            return new LlmGateway.LlmResult(null, LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION);
        });
        ChatOrchestrator orchestrator = new ChatOrchestrator(llmGateway, campusToolAdapter);

        ChatOrchestrationResult result = orchestrator.execute("system", List.of(), "route");

        assertThat(result.reply()).isNull();
        assertThat(result.routeExecution()).isNull();
        verify(campusToolAdapter, never()).findRoute(any());
    }

    @Test
    void neverExecutesRouteFallbackForNavigateRoute() {
        when(llmGateway.generate(anyString(), anyList(), anyString(), any())).thenAnswer(invocation -> {
            recordResolvedRoute(invocation.getArgument(3), SearchCampusIntent.NAVIGATE_ROUTE);
            return new LlmGateway.LlmResult(null, LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION);
        });
        ChatOrchestrator orchestrator = new ChatOrchestrator(llmGateway, campusToolAdapter);

        ChatOrchestrationResult result = orchestrator.execute("system", List.of(), "route");

        assertThat(result.resolvedLocations()).hasSize(2);
        assertThat(result.routeExecution()).isNull();
        verify(campusToolAdapter, never()).findRoute(any());
    }

    private void recordResolvedRoute(ResolvedLocationCollector state, SearchCampusIntent intent) {
        state.record(search("start", SearchCampusRole.START, intent), resolved(11L, "start"));
        state.record(search("end", SearchCampusRole.END, intent), resolved(22L, "end"));
    }

    private SearchCampusToolRequest search(String query, SearchCampusRole role, SearchCampusIntent intent) {
        return new SearchCampusToolRequest(query, 5, role, intent, List.of(RouteCondition.BARRIERFREE));
    }

    private SearchCampusToolResult resolved(long id, String name) {
        return new SearchCampusToolResult(List.of(item(id, name)), false, null);
    }

    private SearchCampusItem item(long id, String name) {
        return new SearchCampusItem(id, ToolLocationType.BUILDING, name, id, name,
                null, null, null, SearchCampusMatchType.EXACT);
    }

    private FindRouteToolResult successfulRoute(long seconds) {
        return new FindRouteToolResult(new FindRouteToolData(seconds, List.of()), null);
    }
}
