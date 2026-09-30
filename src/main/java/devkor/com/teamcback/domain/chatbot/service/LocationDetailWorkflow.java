package devkor.com.teamcback.domain.chatbot.service;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

import devkor.com.teamcback.domain.chatbot.dto.LocationCandidateSelection;
import devkor.com.teamcback.domain.chatbot.dto.LocationCandidateView;
import devkor.com.teamcback.domain.chatbot.dto.LocationDetailPlan;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.LocationDetailToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import devkor.com.teamcback.domain.chatbot.checkpoint.RedisCheckpointSaver;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Deterministic location-detail workflow. The model may parse/select semantics, never execution order or IDs. */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class LocationDetailWorkflow {
    private static final String SEARCH = "search_location";
    private static final String DECIDE = "candidate_decision";
    private static final String ASK = "ask_clarification";
    private static final String SELECT = "select_candidate";
    private static final String LOAD = "get_location_detail";
    private static final String FORMAT = "format_response";
    private static final String OWNER = "owner";
    private static final String QUERY = "query";
    private static final String CANDIDATES = "candidates";
    private static final String SELECTED_ID = "selectedId";
    private static final String SELECTED_TYPE = "selectedType";
    private static final String SELECTED_NAME = "selectedName";
    private static final String REPLY = "reply";
    private static final String INPUT = "input";
    private static final String AWAITING = "awaiting";
    private static final String DETAIL = "detail";

    private final LocationDetailPlanner planner;
    private final CampusToolAdapter adapter;
    private final LocationCandidateSelector selector;
    private final BaseCheckpointSaver saver;
    private final CompiledGraph<LocationGraphState> graph;

    @Autowired
    public LocationDetailWorkflow(LocationDetailPlanner planner, CampusToolAdapter adapter,
                                  LocationCandidateSelector selector, BaseCheckpointSaver saver) {
        this.planner = planner;
        this.adapter = adapter;
        this.selector = selector;
        this.saver = saver;
        try {
            StateGraph<LocationGraphState> state = new StateGraph<>(LocationGraphState.SCHEMA, LocationGraphState::new);
            state.addNode(SEARCH, AsyncNodeAction.node_async((NodeAction<LocationGraphState>) this::search));
            state.addNode(DECIDE, AsyncNodeAction.node_async((NodeAction<LocationGraphState>) this::decide));
            state.addNode(ASK, AsyncNodeAction.node_async((NodeAction<LocationGraphState>) this::ask));
            state.addNode(SELECT, AsyncNodeAction.node_async((NodeAction<LocationGraphState>) this::select));
            state.addNode(LOAD, AsyncNodeAction.node_async((NodeAction<LocationGraphState>) this::load));
            state.addNode(FORMAT, AsyncNodeAction.node_async((NodeAction<LocationGraphState>) this::format));
            state.addEdge(START, SEARCH);
            state.addEdge(SEARCH, DECIDE);
            state.addConditionalEdges(DECIDE,
                    AsyncEdgeAction.edge_async((EdgeAction<LocationGraphState>) this::afterDecision),
                    Map.of(ASK, ASK, LOAD, LOAD, FORMAT, FORMAT, END, END));
            state.addEdge(ASK, SELECT);
            state.addConditionalEdges(SELECT,
                    AsyncEdgeAction.edge_async((EdgeAction<LocationGraphState>) this::afterSelection),
                    Map.of(ASK, ASK, LOAD, LOAD, END, END));
            state.addEdge(LOAD, FORMAT);
            state.addEdge(FORMAT, END);
            graph = state.compile(CompileConfig.builder().checkpointSaver(saver).interruptAfter(ASK)
                    .releaseThread(true).build());
        } catch (GraphStateException exception) {
            throw new IllegalStateException("Unable to compile LOCATION_DETAIL workflow", exception);
        }
    }

    public LocationDetailWorkflow(LocationDetailPlanner planner, CampusToolAdapter adapter,
                                  LocationCandidateSelector selector) {
        this(planner, adapter, selector, new MemorySaver());
    }

    public WorkflowResult handle(UUID sessionId, ChatCaller caller, List<LlmGateway.ConversationMessage> history,
                                 String message) {
        RunnableConfig config = config(sessionId);
        var checkpoint = graph.lastStateOf(config);
        LocationGraphState state;
        if (checkpoint.isPresent()) {
            assertOwner(checkpoint.get().state(), caller);
            state = run(GraphInput.resume(Map.of(INPUT, message)), config);
        } else {
            LocationDetailPlan plan = planner.plan(history, message);
            if (plan == null || !plan.isLocationDetail()) return WorkflowResult.notHandled();
            state = run(GraphInput.args(Map.of(OWNER, caller.key(), QUERY, plan.locationQuery(), INPUT, message)), config);
        }
        WorkflowResult result = result(state);
        if (!result.waiting()) release(config);
        return result;
    }

    public boolean hasPending(UUID sessionId, ChatCaller caller) {
        var checkpoint = graph.lastStateOf(config(sessionId));
        if (checkpoint.isEmpty()) return false;
        assertOwner(checkpoint.get().state(), caller);
        return true;
    }

    public void cancel(UUID sessionId, ChatCaller caller) {
        var config = config(sessionId);
        var checkpoint = graph.lastStateOf(config);
        if (checkpoint.isEmpty()) return;
        assertOwner(checkpoint.get().state(), caller);
        release(config);
    }

    private Map<String, Object> search(LocationGraphState state) {
        SearchCampusToolResult found = adapter.searchCampus(new SearchCampusToolRequest(state.value(QUERY, ""), 5));
        List<Map<String, Object>> candidates = new ArrayList<>();
        if (found != null && found.candidates() != null) {
            for (SearchCampusItem item : found.candidates()) {
                Map<String, Object> value = new HashMap<>();
                value.put("id", item.locationId()); value.put("type", item.locationType().name());
                value.put("name", item.name()); value.put("buildingName", item.buildingName());
                value.put("floor", item.floor() == null ? "" : String.valueOf(item.floor()));
                value.put("placeType", item.placeType() == null ? "" : item.placeType().name());
                candidates.add(value);
            }
        }
        return Map.of(CANDIDATES, candidates, REPLY, candidates.isEmpty()
                ? "요청한 장소를 찾지 못했어요." : "");
    }

    private Map<String, Object> decide(LocationGraphState state) {
        List<Map<String, Object>> candidates = state.value(CANDIDATES, List.of());
        if (candidates.size() == 1) return selected(candidates.get(0));
        if (candidates.size() > 1) return Map.of(AWAITING, "LOCATION_SELECTION");
        return Map.of(AWAITING, "");
    }

    private Map<String, Object> ask(LocationGraphState state) {
        String names = state.value(CANDIDATES, List.<Map<String, Object>>of()).stream()
                .map(c -> String.valueOf(c.get("name"))).distinct().reduce((a, b) -> a + ", " + b).orElse("");
        return Map.of(REPLY, "어느 장소를 말씀하신 건가요? " + names);
    }

    private Map<String, Object> select(LocationGraphState state) {
        List<Map<String, Object>> candidates = state.value(CANDIDATES, List.of());
        List<LocationCandidateView> views = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Map<String, Object> c = candidates.get(i);
            views.add(new LocationCandidateView(i, String.valueOf(c.get("name")),
                    ToolLocationType.valueOf(String.valueOf(c.get("type"))), String.valueOf(c.get("buildingName")),
                    String.valueOf(c.get("floor")), String.valueOf(c.get("placeType"))));
        }
        LocationCandidateSelection selection = selector.select(state.value(INPUT, ""), views);
        Integer index = selection == null ? null : selection.candidateIndex();
        if (selection != null && selection.status() == LocationCandidateSelection.Status.SELECTED
                && index != null && index >= 0 && index < candidates.size()) return selected(candidates.get(index));
        return Map.of(AWAITING, "LOCATION_SELECTION", INPUT, "", REPLY,
                "후보 중 하나를 장소 이름으로 말씀해 주세요.");
    }

    private Map<String, Object> load(LocationGraphState state) {
        Long id = Long.valueOf(state.value(SELECTED_ID, "0"));
        GetLocationDetailToolResult result = adapter.getLocationDetail(new GetLocationDetailToolRequest(
                ToolLocationType.valueOf(state.value(SELECTED_TYPE, "PLACE")), id));
        if (result == null || result.location() == null) return Map.of(REPLY, error(result));
        LocationDetailToolData detail = result.location();
        Map<String, Object> data = new HashMap<>();
        data.put("name", detail.name()); data.put("address", detail.address()); data.put("detail", detail.detail());
        data.put("openNow", detail.openNow()); data.put("weekdayHours", detail.weekdayHours());
        data.put("saturdayHours", detail.saturdayHours()); data.put("sundayHours", detail.sundayHours());
        return Map.of(DETAIL, data);
    }

    private Map<String, Object> format(LocationGraphState state) {
        if (!state.value(REPLY, "").isBlank()) return Map.of();
        Map<String, Object> d = state.value(DETAIL, Map.of());
        StringBuilder reply = new StringBuilder(String.valueOf(d.getOrDefault("name", state.value(SELECTED_NAME, "장소"))));
        if (d.get("openNow") != null) reply.append(" 현재 ").append(Boolean.TRUE.equals(d.get("openNow")) ? "운영 중" : "운영 종료");
        if (d.get("weekdayHours") != null && !String.valueOf(d.get("weekdayHours")).isBlank())
            reply.append("\n평일: ").append(d.get("weekdayHours"));
        if (d.get("address") != null && !String.valueOf(d.get("address")).isBlank())
            reply.append("\n위치: ").append(d.get("address"));
        return Map.of(REPLY, reply.toString());
    }

    private Map<String, Object> selected(Map<String, Object> candidate) {
        return Map.of(SELECTED_ID, String.valueOf(candidate.get("id")), SELECTED_TYPE, candidate.get("type"),
                SELECTED_NAME, candidate.get("name"), AWAITING, "", INPUT, "");
    }

    private String afterDecision(LocationGraphState state) {
        if (state.value(CANDIDATES, List.<Map<String, Object>>of()).isEmpty()) return END;
        if (!state.value(SELECTED_ID, "").isBlank()) return LOAD;
        return ASK;
    }

    private String afterSelection(LocationGraphState state) {
        return state.value(SELECTED_ID, "").isBlank() ? ASK : LOAD;
    }

    private WorkflowResult result(LocationGraphState state) {
        return new WorkflowResult(true, state.value(REPLY, ""), !state.value(AWAITING, "").isBlank());
    }

    private LocationGraphState run(GraphInput input, RunnableConfig config) {
        try { return graph.invoke(input, config).orElseGet(() -> graph.lastStateOf(config).orElseThrow().state()); }
        catch (Exception exception) { throw new IllegalStateException("LOCATION_DETAIL workflow failed", exception); }
    }

    private RunnableConfig config(UUID sessionId) { return RunnableConfig.builder().threadId("location:" + sessionId).build(); }
    private void release(RunnableConfig config) { try { saver.release(config); } catch (Exception e) { throw new IllegalStateException(e); } }
    private void assertOwner(LocationGraphState state, ChatCaller caller) {
        if (!caller.key().equals(state.value(OWNER, "")))
            throw new GlobalException(devkor.com.teamcback.global.response.ResultCode.CHATBOT_SESSION_FORBIDDEN);
    }
    private String error(SearchCampusToolResult result) { return result != null && result.error() != null
            ? result.error().message() : "장소 정보를 확인할 수 없어요."; }
    private String error(GetLocationDetailToolResult result) { return result != null && result.error() != null
            ? result.error().message() : "장소 정보를 확인할 수 없어요."; }

    public record WorkflowResult(boolean handled, String reply, boolean waiting) {
        public static WorkflowResult notHandled() { return new WorkflowResult(false, null, false); }
    }

    public static final class LocationGraphState extends AgentState {
        private static Channel<String> text() { return Channels.base(() -> ""); }
        private static Channel<List<Map<String, Object>>> list() { return Channels.base(ArrayList::new); }
        private static Channel<Map<String, Object>> map() { return Channels.<Map<String, Object>>base(() -> new HashMap<>()); }
        public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
                Map.entry(OWNER, text()), Map.entry(QUERY, text()), Map.entry(CANDIDATES, list()),
                Map.entry(SELECTED_ID, text()), Map.entry(SELECTED_TYPE, text()), Map.entry(SELECTED_NAME, text()),
                Map.entry(REPLY, text()), Map.entry(INPUT, text()), Map.entry(AWAITING, text()), Map.entry(DETAIL, map()));
        public LocationGraphState(Map<String, Object> data) { super(data); }
    }
}
