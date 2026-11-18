package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.dto.LocationCandidateSelection;
import devkor.com.teamcback.domain.chatbot.dto.LocationDetailPlan;
import devkor.com.teamcback.domain.chatbot.dto.PlaceReviewsPlan;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetPlaceReviewsToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.LocationDetailToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.PlaceReviewsToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackendWorkflowTest {
    private static final ChatCaller CALLER = new ChatCaller("user:test", true);

    @Mock LocationDetailPlanner locationPlanner;
    @Mock PlaceReviewsPlanner reviewPlanner;
    @Mock CampusToolAdapter adapter;
    @Mock LocationCandidateSelector selector;

    @Test
    void locationDetailSearchesThenLoadsBackendIdWithoutAskingUserForId() {
        when(locationPlanner.plan(any(), any())).thenReturn(
                new LocationDetailPlan(LocationDetailPlan.Intent.LOCATION_DETAIL, "중앙도서관"));
        when(adapter.searchCampus(any())).thenReturn(new SearchCampusToolResult(List.of(
                new SearchCampusItem(41L, ToolLocationType.BUILDING, "중앙도서관", 41L,
                        "중앙도서관", null, null, null, null)), false, null));
        when(adapter.getLocationDetail(any())).thenReturn(new GetLocationDetailToolResult(
                new LocationDetailToolData(41L, ToolLocationType.BUILDING, "중앙도서관", 41L,
                        "중앙도서관", null, "서울", null, null, true, null,
                        "09:00-21:00", null, null, null, null, null, null), null));

        LocationDetailWorkflow workflow = new LocationDetailWorkflow(locationPlanner, adapter, selector);
        var result = workflow.handle(UUID.randomUUID(), CALLER, List.of(), "중앙도서관 몇시에 닫어?");

        assertThat(result.handled()).isTrue();
        assertThat(result.reply()).contains("중앙도서관").doesNotContain("ID").doesNotContain("아이디");
        verify(adapter).getLocationDetail(any(GetLocationDetailToolRequest.class));
        verify(selector, never()).select(any(), any());
    }

    @Test
    void ambiguousLocationIsInterruptedAndResumesWithCandidateIndex() {
        UUID session = UUID.randomUUID();
        when(locationPlanner.plan(any(), any())).thenReturn(
                new LocationDetailPlan(LocationDetailPlan.Intent.LOCATION_DETAIL, "블루포트"));
        when(adapter.searchCampus(any())).thenReturn(new SearchCampusToolResult(List.of(
                new SearchCampusItem(1L, ToolLocationType.PLACE, "블루포트", 10L, "중앙광장", 1D, null, null, null),
                new SearchCampusItem(2L, ToolLocationType.PLACE, "블루포트", 20L, "하나스퀘어", 1D, null, null, null)), true, null));
        when(selector.select(any(), any())).thenReturn(
                new LocationCandidateSelection(LocationCandidateSelection.Status.SELECTED, 1));
        when(adapter.getLocationDetail(any())).thenReturn(new GetLocationDetailToolResult(
                new LocationDetailToolData(2L, ToolLocationType.PLACE, "블루포트", 20L,
                        "하나스퀘어", 1, null, null, null, false, null,
                        "10:00-20:00", null, null, null, null, null, null), null));

        LocationDetailWorkflow workflow = new LocationDetailWorkflow(locationPlanner, adapter, selector);
        var pending = workflow.handle(session, CALLER, List.of(), "블루포트 몇시에 닫아?");
        var resumed = workflow.handle(session, CALLER, List.of(), "하나스퀘어");

        assertThat(pending.waiting()).isTrue();
        assertThat(resumed.waiting()).isFalse();
        verify(adapter).getLocationDetail(any(GetLocationDetailToolRequest.class));
    }

    @Test
    void invalidCandidateIndexNeverLoadsDetail() {
        when(locationPlanner.plan(any(), any())).thenReturn(
                new LocationDetailPlan(LocationDetailPlan.Intent.LOCATION_DETAIL, "블루포트"));
        when(adapter.searchCampus(any())).thenReturn(new SearchCampusToolResult(List.of(
                new SearchCampusItem(1L, ToolLocationType.PLACE, "블루포트", 10L, "중앙광장", 1D, null, null, null),
                new SearchCampusItem(2L, ToolLocationType.PLACE, "블루포트", 20L, "하나스퀘어", 1D, null, null, null)), true, null));
        when(selector.select(any(), any())).thenReturn(
                new LocationCandidateSelection(LocationCandidateSelection.Status.SELECTED, 99));

        LocationDetailWorkflow workflow = new LocationDetailWorkflow(locationPlanner, adapter, selector);
        UUID session = UUID.randomUUID();
        workflow.handle(session, CALLER, List.of(), "블루포트");
        var result = workflow.handle(session, CALLER, List.of(), "알 수 없는 곳");

        assertThat(result.waiting()).isTrue();
        verify(adapter, never()).getLocationDetail(any());
    }

    @Test
    void reviewWorkflowResolvesPlaceBeforeLoadingReviews() {
        when(reviewPlanner.plan(any(), any())).thenReturn(
                new PlaceReviewsPlan(PlaceReviewsPlan.Intent.PLACE_REVIEWS, "블루포트"));
        when(adapter.searchCampus(any())).thenReturn(new SearchCampusToolResult(List.of(
                new SearchCampusItem(7L, ToolLocationType.PLACE, "블루포트", 20L, "하나스퀘어",
                        1D, null, null, null)), false, null));
        when(adapter.getPlaceReviews(any())).thenReturn(new GetPlaceReviewsToolResult(
                new PlaceReviewsToolData(7L, "블루포트", 4.5, List.of(), List.of(), false), null));

        PlaceReviewsWorkflow workflow = new PlaceReviewsWorkflow(reviewPlanner, adapter, selector);
        var result = workflow.handle(UUID.randomUUID(), CALLER, List.of(), "블루포트 후기 알려줘");

        assertThat(result.reply()).contains("블루포트").contains("4.5");
        verify(adapter).getPlaceReviews(any());
    }

    @Test
    void campusStatusWorkflowDoesNotUseGeneralToolCalling() {
        when(adapter.getCampusStatus()).thenReturn(
                new devkor.com.teamcback.domain.chatbot.tool.dto.CampusStatusToolResult(
                        "2026-1", false, false, null));

        CampusStatusWorkflow workflow = new CampusStatusWorkflow(adapter);
        var result = workflow.handle(UUID.randomUUID(), CALLER, "지금 학기야?");

        assertThat(result.reply()).contains("학기");
        verify(adapter).getCampusStatus();
    }
}
