package devkor.com.teamcback.domain.chatbot.tool;

import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.INVALID_INPUT;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.TEMPORARILY_UNAVAILABLE;
import static devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition.BARRIERFREE;
import static devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType.BUILDING;
import static devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType.COORD;
import static devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType.PLACE;
import static devkor.com.teamcback.domain.routes.entity.Conditions.OPERATING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteCondition;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpoint;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteEndpointType;
import devkor.com.teamcback.domain.chatbot.tool.dto.RouteSectionType;
import devkor.com.teamcback.domain.routes.dto.response.GetRouteRes;
import devkor.com.teamcback.domain.routes.dto.response.PartialRouteRes;
import devkor.com.teamcback.domain.routes.entity.Conditions;
import devkor.com.teamcback.domain.routes.entity.LocationType;
import devkor.com.teamcback.domain.routes.service.RouteService;
import devkor.com.teamcback.domain.search.service.SearchService;
import devkor.com.teamcback.global.exception.exception.AdminException;
import devkor.com.teamcback.global.response.ResultCode;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampusRouteToolAdapterTest {
    @Mock SearchService searchService;
    @Mock RouteService routeService;
    private CampusToolAdapter adapter;

    @BeforeEach
    void setUp() {
        ChatbotProperties properties = new ChatbotProperties(true,
                new ChatbotProperties.Llm("google", "gemini-2.5-flash-lite", 500, 8),
                new ChatbotProperties.Agent(6, 5, 60),
                new ChatbotProperties.Tools(new ChatbotProperties.Limits(5, 10),
                        new ChatbotProperties.Limits(10, 20), 7,
                        new ChatbotProperties.Limits(5, 10)),
                new ChatbotProperties.RateLimit(30, 10, 5, "Asia/Seoul"));
        adapter = new CampusToolAdapter(searchService, routeService, null, null, null, null, null, properties);
    }

    @Test
    void mapsBuildingRouteAndRemovesRawCoordinatesAndNodeIds() {
        PartialRouteRes outdoor = new PartialRouteRes(List.of(List.of(37.5, 127.0, 999.0)));
        outdoor.setInfo("중앙도서관으로 이동하세요");
        when(routeService.findRoute(LocationType.BUILDING, 5L, null, null,
                LocationType.BUILDING, 11L, null, null, List.of()))
                .thenReturn(List.of(new GetRouteRes(420L, List.of(outdoor))));

        var result = adapter.findRoute(request(endpoint(BUILDING, 5L), endpoint(BUILDING, 11L), List.of()));

        assertThat(result.error()).isNull();
        assertThat(result.route().estimatedDurationSeconds()).isEqualTo(420L);
        assertThat(result.route().steps()).singleElement().satisfies(step -> {
            assertThat(step.sectionType()).isEqualTo(RouteSectionType.OUTDOOR);
            assertThat(step.buildingId()).isNull();
            assertThat(step.floor()).isNull();
            assertThat(step.instruction()).isEqualTo("중앙도서관으로 이동하세요");
        });
        assertThat(Arrays.stream(result.route().getClass().getRecordComponents()).map(RecordComponent::getName))
                .doesNotContain("route", "coordinates", "nodeId", "path");
        assertThat(Arrays.stream(result.route().steps().get(0).getClass().getRecordComponents())
                .map(RecordComponent::getName)).doesNotContain("route", "coordinates", "nodeId");
    }

    @Test
    void mapsPlaceToBuildingEndpointTypes() {
        when(routeService.findRoute(LocationType.PLACE, 31L, null, null,
                LocationType.BUILDING, 11L, null, null, List.of(OPERATING)))
                .thenReturn(List.of(new GetRouteRes(100L, List.of())));

        var result = adapter.findRoute(request(endpoint(PLACE, 31L), endpoint(BUILDING, 11L),
                List.of(RouteCondition.OPERATING)));

        assertThat(result.error()).isNull();
        verify(routeService).findRoute(LocationType.PLACE, 31L, null, null,
                LocationType.BUILDING, 11L, null, null, List.of(OPERATING));
    }

    @Test
    void mapsCurrentCoordinateAndBarrierFreeConditionExactly() {
        RouteEndpoint start = new RouteEndpoint(COORD, null, 37.5861, 127.0290);
        when(routeService.findRoute(LocationType.COORD, null, 37.5861, 127.0290,
                LocationType.BUILDING, 11L, null, null, List.of(Conditions.BARRIERFREE)))
                .thenReturn(List.of(new GetRouteRes(300L, List.of())));

        var result = adapter.findRoute(request(start, endpoint(BUILDING, 11L), List.of(BARRIERFREE)));

        assertThat(result.error()).isNull();
        verify(routeService).findRoute(LocationType.COORD, null, 37.5861, 127.0290,
                LocationType.BUILDING, 11L, null, null, List.of(Conditions.BARRIERFREE));
    }

    @Test
    void rejectsInvalidCoordinateAndMixedEndpointFieldsBeforeDomainCall() {
        var missingLongitude = new RouteEndpoint(COORD, null, 37.5, null);
        var mixedBuilding = new RouteEndpoint(BUILDING, 5L, 37.5, 127.0);

        assertThat(adapter.findRoute(request(missingLongitude, endpoint(BUILDING, 11L), List.of()))
                .error().code()).isEqualTo(INVALID_INPUT);
        assertThat(adapter.findRoute(request(mixedBuilding, endpoint(BUILDING, 11L), List.of()))
                .error().code()).isEqualTo(INVALID_INPUT);
        verify(routeService, never()).findRoute(any(), any(), any(), any(), any(), any(), any(), any(), anyList());
    }

    @Test
    void toolEnumsDoNotExposeNodeOrInnerRoute() {
        assertThat(RouteEndpointType.values()).extracting(Enum::name)
                .containsExactlyInAnyOrder("BUILDING", "PLACE", "COORD");
        assertThat(RouteCondition.values()).extracting(Enum::name)
                .containsExactlyInAnyOrder("BARRIERFREE", "SHUTTLE", "STUDENTCARD", "OPERATING");
    }

    @Test
    void internalRouteFailureDoesNotLeakAdminDetails() {
        when(routeService.findRoute(eq(LocationType.BUILDING), eq(5L), isNull(), isNull(),
                eq(LocationType.BUILDING), eq(11L), isNull(), isNull(), anyList()))
                .thenThrow(new AdminException(ResultCode.INCORRECT_NODE_DATA, "node 999 database secret"));

        var result = adapter.findRoute(request(endpoint(BUILDING, 5L), endpoint(BUILDING, 11L), List.of()));

        assertThat(result.error().code()).isEqualTo(TEMPORARILY_UNAVAILABLE);
        assertThat(result.error().message()).doesNotContain("999", "database", "secret");
    }

    private FindRouteToolRequest request(RouteEndpoint start, RouteEndpoint end, List<RouteCondition> conditions) {
        return new FindRouteToolRequest(start, end, conditions);
    }

    private RouteEndpoint endpoint(RouteEndpointType type, Long id) {
        return new RouteEndpoint(type, id, null, null);
    }
}
