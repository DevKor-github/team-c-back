package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.dto.CrowdStatusPlan;
import devkor.com.teamcback.domain.chatbot.crowd.CrowdPlaceCandidate;
import devkor.com.teamcback.domain.chatbot.crowd.CrowdTargetResolver;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.search.ChatbotCampusSearchService;
import devkor.com.teamcback.domain.chatbot.search.ChatbotSearchCandidate;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.CrowdLevel;
import devkor.com.teamcback.domain.chatbot.tool.dto.CrowdStatusToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCrowdStatusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCrowdStatusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrowdStatusWorkflowTest {
    @Mock CrowdStatusPlanner planner;
    @Mock ChatbotCampusSearchService searchService;
    @Mock CampusToolAdapter campusToolAdapter;
    @Mock CrowdTargetResolver targetResolver;

    @Test
    void uniquePlaceIsLoadedByBackendWithoutToolCalling() {
        UUID sessionId = UUID.randomUUID();
        when(planner.plan(any(), any())).thenReturn(new CrowdStatusPlan(
                CrowdStatusPlan.Intent.CROWD_STATUS, "열람실"));
        when(searchService.search("열람실")).thenReturn(List.of(
                new ChatbotSearchCandidate(77L, ToolLocationType.PLACE, "중앙도서관 열람실",
                        1L, "중앙도서관", 2D, null, null, 300, "열람실", false)));
        when(campusToolAdapter.getCrowdStatus(any())).thenReturn(new GetCrowdStatusToolResult(
                new CrowdStatusToolData(77L, 10, 100, CrowdLevel.AVAILABLE, null, false, null), null));

        CrowdStatusWorkflow workflow = new CrowdStatusWorkflow(planner, searchService, campusToolAdapter);
        CrowdStatusWorkflow.CrowdWorkflowResult result = workflow.handle(sessionId,
                new ChatCaller("user:1", true), List.of(), "열람실 혼잡도");

        assertThat(result.handled()).isTrue();
        assertThat(result.reply()).contains("중앙도서관 열람실").contains("AVAILABLE");
        verify(campusToolAdapter).getCrowdStatus(new GetCrowdStatusToolRequest(77L, false));
    }

    @Test
    void buildingCandidateIsConfirmedButNeverUsedAsPlaceId() {
        UUID sessionId = UUID.randomUUID();
        when(planner.plan(any(), any())).thenReturn(new CrowdStatusPlan(
                CrowdStatusPlan.Intent.CROWD_STATUS, "미래관"));
        when(searchService.search("미래관")).thenReturn(List.of(
                new ChatbotSearchCandidate(33L, ToolLocationType.BUILDING, "SK미래관",
                        33L, "SK미래관", null, null, null, 200, "미래관", false)));

        CrowdStatusWorkflow workflow = new CrowdStatusWorkflow(planner, searchService, campusToolAdapter);
        ChatCaller caller = new ChatCaller("user:1", true);
        CrowdStatusWorkflow.CrowdWorkflowResult first = workflow.handle(sessionId, caller, List.of(), "미래관 혼잡도");
        CrowdStatusWorkflow.CrowdWorkflowResult second = workflow.handle(sessionId, caller, List.of(), "맞아");

        assertThat(first.reply()).isEqualTo("SK미래관을(를) 말씀하시나요?");
        assertThat(second.reply()).contains("확인 가능한 혼잡도 장소");
        verify(campusToolAdapter, never()).getCrowdStatus(any());
    }

    @Test
    void nonCrowdRequestLeavesGeneralChatPathAvailable() {
        when(planner.plan(any(), any())).thenReturn(CrowdStatusPlan.other());

        CrowdStatusWorkflow workflow = new CrowdStatusWorkflow(planner, searchService, campusToolAdapter);
        CrowdStatusWorkflow.CrowdWorkflowResult result = workflow.handle(UUID.randomUUID(),
                new ChatCaller("user:1", true), List.of(), "오늘 학식 알려줘");

        assertThat(result.handled()).isFalse();
        verify(searchService, never()).search(any());
        verify(campusToolAdapter, never()).getCrowdStatus(any());
    }

    @Test
    void ambiguousCandidatesAreNotSelectedByTheGraph() {
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = new ChatCaller("user:1", true);
        when(planner.plan(any(), any())).thenReturn(new CrowdStatusPlan(
                CrowdStatusPlan.Intent.CROWD_STATUS, "중도"));
        when(searchService.search("중도")).thenReturn(List.of(
                new ChatbotSearchCandidate(1L, ToolLocationType.PLACE, "중앙도서관(신관)",
                        1L, "중앙도서관(신관)", null, null, null, 290, "중도", false),
                new ChatbotSearchCandidate(2L, ToolLocationType.PLACE, "중앙도서관(대학원)",
                        2L, "중앙도서관(대학원)", null, null, null, 290, "중도", false)));

        CrowdStatusWorkflow workflow = new CrowdStatusWorkflow(planner, searchService, campusToolAdapter);
        CrowdStatusWorkflow.CrowdWorkflowResult first = workflow.handle(sessionId, caller, List.of(), "중도 혼잡도");
        CrowdStatusWorkflow.CrowdWorkflowResult second = workflow.handle(sessionId, caller, List.of(), "맞아");

        assertThat(first.reply()).contains("중앙도서관(신관)").contains("중앙도서관(대학원)");
        assertThat(second.reply()).isNotBlank();
        verify(campusToolAdapter, never()).getCrowdStatus(any());
    }

    @Test
    void concreteCrowdPlaceBeatsPartialBuilding() {
        UUID sessionId = UUID.randomUUID();
        when(planner.plan(any(), any())).thenReturn(new CrowdStatusPlan(
                CrowdStatusPlan.Intent.CROWD_STATUS, "SK미래관 B1층 라운지"));
        when(searchService.search("SK미래관 B1층 라운지")).thenReturn(List.of(
                new ChatbotSearchCandidate(33L, ToolLocationType.BUILDING, "SK미래관",
                        33L, "SK미래관", null, null, null, 200, "미래관", true),
                new ChatbotSearchCandidate(4385L, ToolLocationType.PLACE, "라운지",
                        33L, "SK미래관", -1D, null, null, 190, "라운지", true)));
        CrowdPlaceCandidate place = new CrowdPlaceCandidate(4385L, "SK미래관 B1층 라운지", "라운지",
                -1D, null, 33L, "SK미래관");
        when(targetResolver.resolve(any(), any())).thenReturn(CrowdTargetResolver.Resolution.of(List.of(place)));
        when(campusToolAdapter.getCrowdStatus(any())).thenReturn(new GetCrowdStatusToolResult(
                new CrowdStatusToolData(4385L, 3, 20, CrowdLevel.AVAILABLE, null, false, null), null));

        CrowdStatusWorkflow workflow = new CrowdStatusWorkflow(planner, searchService, campusToolAdapter, targetResolver);
        CrowdStatusWorkflow.CrowdWorkflowResult result = workflow.handle(sessionId,
                new ChatCaller("user:1", true), List.of(), "SK미래관 B1층 라운지 혼잡도");

        assertThat(result.reply()).contains("SK미래관 B1층 라운지");
        verify(campusToolAdapter).getCrowdStatus(new GetCrowdStatusToolRequest(4385L, false));
    }

    @Test
    void buildingConfirmationMovesToCrowdPlaceSelection() {
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = new ChatCaller("user:1", true);
        when(planner.plan(any(), any())).thenReturn(new CrowdStatusPlan(
                CrowdStatusPlan.Intent.CROWD_STATUS, "미래관"));
        when(searchService.search("미래관")).thenReturn(List.of(
                new ChatbotSearchCandidate(33L, ToolLocationType.BUILDING, "SK미래관",
                        33L, "SK미래관", null, null, null, 200, "미래관", false)));
        List<CrowdPlaceCandidate> places = List.of(
                new CrowdPlaceCandidate(4385L, "SK미래관 B1층 라운지", "라운지", -1D, null, 33L, "SK미래관"),
                new CrowdPlaceCandidate(4429L, "SK미래관 3층 라운지", "라운지", 3D, null, 33L, "SK미래관"));
        when(targetResolver.resolve(any(), any())).thenReturn(CrowdTargetResolver.Resolution.of(places));
        when(targetResolver.byBuilding(33L)).thenReturn(CrowdTargetResolver.Resolution.of(places));
        when(targetResolver.resolveWithinBuilding("B1층 라운지", 33L))
                .thenReturn(CrowdTargetResolver.Resolution.of(List.of(places.get(0))));
        when(targetResolver.resolveWithinBuilding("응", 33L))
                .thenReturn(CrowdTargetResolver.Resolution.notFound());
        when(campusToolAdapter.getCrowdStatus(any())).thenReturn(new GetCrowdStatusToolResult(
                new CrowdStatusToolData(4385L, 3, 20, CrowdLevel.AVAILABLE, null, false, null), null));

        CrowdStatusWorkflow workflow = new CrowdStatusWorkflow(planner, searchService, campusToolAdapter, targetResolver);
        assertThat(workflow.handle(sessionId, caller, List.of(), "미래관 혼잡도").reply())
                .contains("SK미래관");
        assertThat(workflow.handle(sessionId, caller, List.of(), "응").reply())
                .contains("B1층 라운지").contains("3층 라운지");
        assertThat(workflow.handle(sessionId, caller, List.of(), "응").reply()).isNotBlank();
        verify(campusToolAdapter, never()).getCrowdStatus(any());
        assertThat(workflow.handle(sessionId, caller, List.of(), "B1층 라운지").reply())
                .contains("SK미래관 B1층 라운지");
        verify(campusToolAdapter).getCrowdStatus(new GetCrowdStatusToolRequest(4385L, false));
    }

    @Test
    void placeSelectionClearsPreviousPromptAndLoadsSelectedCrowd() {
        UUID sessionId = UUID.randomUUID();
        ChatCaller caller = new ChatCaller("user:1", true);
        when(planner.plan(any(), any())).thenReturn(new CrowdStatusPlan(
                CrowdStatusPlan.Intent.CROWD_STATUS, "Mirae Building"));
        when(searchService.search("Mirae Building")).thenReturn(List.of(
                new ChatbotSearchCandidate(33L, ToolLocationType.BUILDING, "Mirae Building",
                        33L, "Mirae Building", null, null, null, 300, "Mirae", false)));
        List<CrowdPlaceCandidate> places = List.of(
                new CrowdPlaceCandidate(4385L, "Mirae Building B1 Lounge", "Lounge", -1D,
                        null, 33L, "Mirae Building"),
                new CrowdPlaceCandidate(4443L, "Mirae Building Blue Port", "Blue Port", 3D,
                        null, 33L, "Mirae Building"));
        when(targetResolver.resolve(any(), any())).thenReturn(CrowdTargetResolver.Resolution.of(places));
        when(targetResolver.resolveWithinBuilding("Mirae Building Blue Port", 33L))
                .thenReturn(CrowdTargetResolver.Resolution.of(List.of(places.get(1))));
        when(campusToolAdapter.getCrowdStatus(any())).thenReturn(new GetCrowdStatusToolResult(
                new CrowdStatusToolData(4443L, 10, 100, CrowdLevel.AVAILABLE, null, false, null), null));

        CrowdStatusWorkflow workflow = new CrowdStatusWorkflow(planner, searchService, campusToolAdapter, targetResolver);
        CrowdStatusWorkflow.CrowdWorkflowResult first = workflow.handle(sessionId, caller, List.of(), "Mirae Building");
        CrowdStatusWorkflow.CrowdWorkflowResult second = workflow.handle(sessionId, caller, List.of(), "Mirae Building Blue Port");

        assertThat(first.waiting()).isTrue();
        assertThat(second.reply()).contains("AVAILABLE");
        verify(targetResolver).resolveWithinBuilding("Mirae Building Blue Port", 33L);
        verify(campusToolAdapter).getCrowdStatus(new GetCrowdStatusToolRequest(4443L, false));
    }
}
