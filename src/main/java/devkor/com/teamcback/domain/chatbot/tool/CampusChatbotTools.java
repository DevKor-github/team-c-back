package devkor.com.teamcback.domain.chatbot.tool;

import devkor.com.teamcback.domain.chatbot.service.ChatbotToolCallLimiter;
import devkor.com.teamcback.domain.chatbot.service.ResolvedLocationCollector;
import devkor.com.teamcback.domain.chatbot.tool.dto.CampusStatusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindFacilitiesToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindFacilitiesToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindRouteToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCafeteriaMenuToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCafeteriaMenuToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCrowdStatusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCrowdStatusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetPlaceReviewsToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetPlaceReviewsToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetRoomCoursesToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetRoomCoursesToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
@Slf4j
public class CampusChatbotTools {
    private final CampusToolAdapter adapter;
    private final ChatbotToolCallLimiter toolCallLimiter;
    private final ResolvedLocationCollector resolvedLocationCollector;
    private final ChatbotToolCallLimiter.Scope toolCallScope;

    @Autowired
    public CampusChatbotTools(CampusToolAdapter adapter, ChatbotToolCallLimiter toolCallLimiter) {
        this(adapter, toolCallLimiter, null, null);
    }

    private CampusChatbotTools(CampusToolAdapter adapter, ChatbotToolCallLimiter toolCallLimiter,
                               ResolvedLocationCollector resolvedLocationCollector,
                               ChatbotToolCallLimiter.Scope toolCallScope) {
        this.adapter = adapter;
        this.toolCallLimiter = toolCallLimiter;
        this.resolvedLocationCollector = resolvedLocationCollector;
        this.toolCallScope = toolCallScope;
    }

    /** Creates an isolated Tool object for one LLM invocation. */
    public CampusChatbotTools forRequest(ResolvedLocationCollector collector,
                                         ChatbotToolCallLimiter.Scope scope) {
        return new CampusChatbotTools(adapter, toolCallLimiter, collector, scope);
    }

    private void beforeToolCall() {
        if (toolCallScope != null) {
            toolCallScope.beforeToolCall();
        }
    }

    @Tool(name = "searchCampus", description = "Resolve a named campus building or place to real BUILDING/PLACE candidates. "
            + "Use when an ID is unknown. For a route request, search both endpoints and set role START or END, "
            + "intent NAVIGATE_ROUTE for a UI action or TEXT_ROUTE for textual route computation. "
            + "Preserve the user's specific place wording. Use only returned candidates; never invent or select an "
            + "ambiguous candidate. For NAVIGATE_ROUTE, resolve START and END and do not call findRoute; the backend "
            + "creates the UI action. For TEXT_ROUTE, resolve both endpoints before findRoute.")
    public SearchCampusToolResult searchCampus(
            @ToolParam(description = "Place name query, optional START/END role, route intent, and supported conditions")
            SearchCampusToolRequest request) {
        log.info("chatbot_tool searchCampus query={} limit={} role={} intent={}",
                request.query(), request.limit(), request.role(), request.intent());
        beforeToolCall();
        SearchCampusToolResult result = adapter.searchCampus(request);
        if (resolvedLocationCollector != null) {
            resolvedLocationCollector.record(request, result);
        }
        return result;
    }

    @Tool(name = "getLocationDetail", description = "Get detail or operating information for a resolved BUILDING/PLACE ID. "
            + "Use searchCampus first when the ID is unknown; do not use this to calculate a route.")
    public GetLocationDetailToolResult getLocationDetail(
            @ToolParam(description = "Resolved BUILDING or PLACE type and ID") GetLocationDetailToolRequest request) {
        beforeToolCall();
        return adapter.getLocationDetail(request);
    }

    @Tool(name = "findFacilities", description = "Find campus facilities by type/building/floor. "
            + "Resolve an unknown building with searchCampus first; do not use this for route navigation.")
    public FindFacilitiesToolResult findFacilities(
            @ToolParam(description = "Facility type and optional building/floor filters") FindFacilitiesToolRequest request) {
        beforeToolCall();
        return adapter.findFacilities(request);
    }

    @Tool(name = "findRoute", description = "Compute a textual route result with duration/steps. "
            + "Use searchCampus for unknown BUILDING/PLACE IDs first. Do not call this only to open the UI route screen; "
            + "call this only for TEXT_ROUTE after START and END are uniquely resolved. NAVIGATE_ROUTE UI actions are "
            + "assembled by the backend after endpoint resolution.")
    public FindRouteToolResult findRoute(
            @ToolParam(description = "Start/end endpoint and optional BARRIERFREE, SHUTTLE, STUDENTCARD, OPERATING conditions")
            FindRouteToolRequest request) {
        beforeToolCall();
        if (resolvedLocationCollector != null && resolvedLocationCollector.hasNavigateIntent()) {
            return new FindRouteToolResult(null,
                    devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolError.of(
                            devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.UNSUPPORTED));
        }
        FindRouteToolResult result = adapter.findRoute(request);
        if (resolvedLocationCollector != null) {
            resolvedLocationCollector.recordRouteExecution(request, result);
        }
        return result;
    }

    @Tool(name = "getCafeteriaMenu", description = "Get a cafeteria meal menu for a resolved PLACE ID and date range. "
            + "Use searchCampus first; do not invent missing menu data.")
    public GetCafeteriaMenuToolResult getCafeteriaMenu(
            @ToolParam(description = "Cafeteria PLACE ID and requested dates") GetCafeteriaMenuToolRequest request) {
        beforeToolCall();
        return adapter.getCafeteriaMenu(request);
    }

    @Tool(name = "getRoomCourses", description = "Get courses scheduled in a resolved classroom PLACE ID. "
            + "This is not a personal timetable.")
    public GetRoomCoursesToolResult getRoomCourses(
            @ToolParam(description = "Classroom PLACE ID and optional weekday") GetRoomCoursesToolRequest request) {
        beforeToolCall();
        return adapter.getRoomCourses(request);
    }

    @Tool(name = "getCampusStatus", description = "Get current campus term, vacation, and Koyeon status. No location ID is needed.")
    public CampusStatusToolResult getCampusStatus() {
        beforeToolCall();
        return adapter.getCampusStatus();
    }

    @Tool(name = "getCrowdStatus", description = "Get sensor-backed crowd status for a resolved PLACE ID. "
            + "Do not guess when sensor data is unavailable.")
    public GetCrowdStatusToolResult getCrowdStatus(
            @ToolParam(description = "PLACE ID and crowd-pattern option") GetCrowdStatusToolRequest request) {
        beforeToolCall();
        return adapter.getCrowdStatus(request);
    }

    @Tool(name = "getPlaceReviews", description = "Get compact reviews for a resolved PLACE ID. "
            + "Do not expose or invent review-author identity.")
    public GetPlaceReviewsToolResult getPlaceReviews(
            @ToolParam(description = "PLACE ID and review limit") GetPlaceReviewsToolRequest request) {
        beforeToolCall();
        return adapter.getPlaceReviews(request);
    }
}
