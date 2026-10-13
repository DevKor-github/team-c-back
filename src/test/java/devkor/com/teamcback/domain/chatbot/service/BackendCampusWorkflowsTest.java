package devkor.com.teamcback.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import devkor.com.teamcback.domain.chatbot.dto.MenuPlan;
import devkor.com.teamcback.domain.chatbot.dto.RoomCoursePlan;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.*;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BackendCampusWorkflowsTest {
    private final ChatCaller caller = new ChatCaller("ip:test", false);

    @Test
    void uniqueCafeteriaIsResolvedByBackendAndLoadedWithoutCandidateIdInPlanner() {
        MenuPlanner planner = mock(MenuPlanner.class);
        CampusToolAdapter adapter = mock(CampusToolAdapter.class);
        LocationCandidateSelector selector = mock(LocationCandidateSelector.class);
        when(planner.plan(anyList(), anyString())).thenReturn(new MenuPlan(MenuPlan.Intent.MENU, "학생회관", "오늘"));
        when(adapter.searchCampus(any())).thenReturn(new SearchCampusToolResult(
                List.of(new SearchCampusItem(9757L, ToolLocationType.PLACE, "학생회관 식당", 10L,
                        "학생회관", 1.0, PlaceType.CAFETERIA, null, SearchCampusMatchType.EXACT)), false, null));
        when(adapter.getCafeteriaMenu(any())).thenReturn(new GetCafeteriaMenuToolResult(
                new GetCafeteriaMenuToolData(9757L, "학생회관 식당", List.of(
                        new CafeteriaMenuDay(LocalDate.now(), List.of(new CafeteriaMealToolItem("중식", "비빔밥"))))), null));

        MenuWorkflow workflow = new MenuWorkflow(planner, adapter, selector);
        var result = workflow.handle(UUID.randomUUID(), caller, List.of(), "오늘 학생회관 학식");

        assertThat(result.handled()).isTrue();
        assertThat(result.reply()).contains("비빔밥");
        verify(adapter).getCafeteriaMenu(argThat(request -> request.placeId().equals(9757L)));
        verifyNoInteractions(selector);
    }

    @Test
    void roomWorkflowDoesNotExecuteForNonClassroomPlace() {
        RoomCoursePlanner planner = mock(RoomCoursePlanner.class);
        CampusToolAdapter adapter = mock(CampusToolAdapter.class);
        LocationCandidateSelector selector = mock(LocationCandidateSelector.class);
        when(planner.plan(anyList(), anyString())).thenReturn(new RoomCoursePlan(
                RoomCoursePlan.Intent.ROOM_COURSE, "블루포트", "오늘"));
        when(adapter.searchCampus(any())).thenReturn(new SearchCampusToolResult(
                List.of(new SearchCampusItem(42L, ToolLocationType.PLACE, "블루포트", 10L,
                        "SK미래관", 1.0, PlaceType.CAFE, null, SearchCampusMatchType.EXACT)), false, null));

        RoomCourseWorkflow workflow = new RoomCourseWorkflow(planner, adapter, selector);
        var result = workflow.handle(UUID.randomUUID(), caller, List.of(), "블루포트 수업");

        assertThat(result.reply()).contains("강의실");
        verify(adapter, never()).getRoomCourses(any());
    }

    @Test
    void invalidCandidateIndexCannotChooseACafeteria() {
        MenuPlanner planner = mock(MenuPlanner.class);
        CampusToolAdapter adapter = mock(CampusToolAdapter.class);
        LocationCandidateSelector selector = mock(LocationCandidateSelector.class);
        when(planner.plan(anyList(), anyString())).thenReturn(new MenuPlan(MenuPlan.Intent.MENU, "식당", "오늘"));
        when(adapter.searchCampus(any())).thenReturn(new SearchCampusToolResult(List.of(
                new SearchCampusItem(1L, ToolLocationType.PLACE, "A 식당", 10L, "A관", 1.0, PlaceType.CAFETERIA, null, SearchCampusMatchType.PARTIAL),
                new SearchCampusItem(2L, ToolLocationType.PLACE, "B 식당", 11L, "B관", 1.0, PlaceType.CAFETERIA, null, SearchCampusMatchType.PARTIAL)), true, null));
        when(selector.select(anyString(), anyList())).thenReturn(new devkor.com.teamcback.domain.chatbot.dto.LocationCandidateSelection(
                devkor.com.teamcback.domain.chatbot.dto.LocationCandidateSelection.Status.SELECTED, 99));

        MenuWorkflow workflow = new MenuWorkflow(planner, adapter, selector);
        UUID session = UUID.randomUUID();
        var first = workflow.handle(session, caller, List.of(), "식당 메뉴");
        var second = workflow.handle(session, caller, List.of(), "잘못된 후보");

        assertThat(first.waiting()).isTrue();
        assertThat(second.waiting()).isTrue();
        verify(adapter, never()).getCafeteriaMenu(any());
    }
}
