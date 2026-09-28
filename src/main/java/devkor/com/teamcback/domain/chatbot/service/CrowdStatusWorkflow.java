package devkor.com.teamcback.domain.chatbot.service;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

import devkor.com.teamcback.domain.chatbot.dto.CrowdStatusPlan;
import devkor.com.teamcback.domain.chatbot.dto.CrowdCandidateSelection;
import devkor.com.teamcback.domain.chatbot.dto.CrowdCandidateView;
import devkor.com.teamcback.domain.chatbot.crowd.CrowdPlaceCandidate;
import devkor.com.teamcback.domain.chatbot.crowd.CrowdTargetResolver;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.search.ChatbotCampusSearchService;
import devkor.com.teamcback.domain.chatbot.search.ChatbotSearchCandidate;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.CrowdStatusToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCrowdStatusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCrowdStatusToolResult;
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
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Backend-owned CROWD_STATUS proof of concept. It deliberately does not expose any campus tool to the model.
 * MemorySaver is intentionally used only for this POC; production persistence belongs in a dedicated saver.
 */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class CrowdStatusWorkflow {
    private static final String RESOLVE = "resolve_location";
    private static final String ASK = "ask_confirmation";
    private static final String CONFIRM = "confirmation_check";
    private static final String TARGET = "resolve_crowd_target";
    private static final String LOAD = "load_crowd";
    private static final String FORMAT = "format_reply";
    private static final String OWNER = "owner";
    private static final String QUERY = "locationQuery";
    private static final String CANDIDATES = "candidates";
    private static final String SELECTED_ID = "selectedId";
    private static final String SELECTED_TYPE = "selectedType";
    private static final String SELECTED_NAME = "selectedName";
    private static final String REPLY = "reply";
    private static final String CONFIRMATION = "confirmation";
    private static final String CROWD_LEVEL = "crowdLevel";
    private static final String PEOPLE = "people";
    private static final String CAPACITY = "capacity";
    private static final String STALE = "stale";
    private static final String BUILDING_ID = "buildingId";
    private static final String AWAITING = "awaiting";
    private static final String BUILDING_CONFIRMATION = "BUILDING_CONFIRMATION";
    private static final String PLACE_SELECTION = "PLACE_SELECTION";

    private final CrowdStatusPlanner planner;
    private final ChatbotCampusSearchService searchService;
    private final CampusToolAdapter campusToolAdapter;
    private final CrowdTargetResolver targetResolver;
    private final CrowdCandidateSelector candidateSelector;
    private final MemorySaver saver;
    private final CompiledGraph<CrowdGraphState> graph;

    CrowdStatusWorkflow(CrowdStatusPlanner planner, ChatbotCampusSearchService searchService,
                        CampusToolAdapter campusToolAdapter) {
        this(planner, searchService, campusToolAdapter, null, null);
    }

    public CrowdStatusWorkflow(CrowdStatusPlanner planner, ChatbotCampusSearchService searchService,
                               CampusToolAdapter campusToolAdapter, CrowdTargetResolver targetResolver) {
        this(planner, searchService, campusToolAdapter, targetResolver, null);
    }

    @Autowired
    public CrowdStatusWorkflow(CrowdStatusPlanner planner, ChatbotCampusSearchService searchService,
                               CampusToolAdapter campusToolAdapter, CrowdTargetResolver targetResolver,
                               CrowdCandidateSelector candidateSelector) {
        this.planner = planner;
        this.searchService = searchService;
        this.campusToolAdapter = campusToolAdapter;
        this.targetResolver = targetResolver;
        this.candidateSelector = candidateSelector;
        try {
            this.saver = new MemorySaver();
            StateGraph<CrowdGraphState> stateGraph = new StateGraph<>(CrowdGraphState.SCHEMA,
                    CrowdGraphState::new);
            stateGraph.addNode(RESOLVE, AsyncNodeAction.node_async((NodeAction<CrowdGraphState>) this::resolveLocation));
            stateGraph.addNode(ASK, AsyncNodeAction.node_async((NodeAction<CrowdGraphState>) this::askConfirmation));
            stateGraph.addNode(CONFIRM, AsyncNodeAction.node_async((NodeAction<CrowdGraphState>) this::checkConfirmation));
            stateGraph.addNode(TARGET, AsyncNodeAction.node_async((NodeAction<CrowdGraphState>) this::resolveCrowdTarget));
            stateGraph.addNode(LOAD, AsyncNodeAction.node_async((NodeAction<CrowdGraphState>) this::loadCrowd));
            stateGraph.addNode(FORMAT, AsyncNodeAction.node_async((NodeAction<CrowdGraphState>) this::formatReply));
            stateGraph.addEdge(START, RESOLVE);
            stateGraph.addConditionalEdges(RESOLVE, AsyncEdgeAction.edge_async((EdgeAction<CrowdGraphState>) this::nextAfterResolve), Map.of(
                    ASK, ASK, TARGET, TARGET, END, END));
            stateGraph.addEdge(ASK, CONFIRM);
            stateGraph.addConditionalEdges(CONFIRM,
                    AsyncEdgeAction.edge_async((EdgeAction<CrowdGraphState>) this::nextAfterConfirmation),
                    Map.of(TARGET, TARGET, ASK, ASK, END, END));
            stateGraph.addConditionalEdges(TARGET,
                    AsyncEdgeAction.edge_async((EdgeAction<CrowdGraphState>) this::nextAfterTarget),
                    Map.of(LOAD, LOAD, FORMAT, FORMAT, TARGET, TARGET, ASK, ASK, END, END));
            stateGraph.addEdge(LOAD, FORMAT);
            stateGraph.addEdge(FORMAT, END);
            this.graph = stateGraph.compile(CompileConfig.builder()
                    .checkpointSaver(this.saver)
                    .interruptAfter(ASK)
                    .releaseThread(true)
                    .build());
        } catch (GraphStateException exception) {
            throw new IllegalStateException("Unable to compile CROWD_STATUS workflow", exception);
        }
    }

    public CrowdWorkflowResult handle(UUID sessionId, ChatCaller caller,
                                       List<LlmGateway.ConversationMessage> history, String userMessage) {
        RunnableConfig config = RunnableConfig.builder().threadId(sessionId.toString()).build();
        var checkpoint = graph.lastStateOf(config);
        if (checkpoint.isPresent()) {
            CrowdGraphState state = checkpoint.get().state();
            if (!caller.key().equals(state.value(OWNER, ""))) {
                throw new GlobalException(devkor.com.teamcback.global.response.ResultCode.CHATBOT_SESSION_FORBIDDEN);
            }
            CrowdWorkflowResult resumed = result(run(GraphInput.resume(Map.of(CONFIRMATION, userMessage)), config));
            releaseIfCompleted(resumed, config);
            return resumed;
        }

        CrowdStatusPlan plan = planner.plan(history, userMessage);
        if (!plan.isCrowdStatus()) {
            return CrowdWorkflowResult.notHandled();
        }
        Map<String, Object> input = Map.of(
                OWNER, caller.key(),
                QUERY, plan.locationQuery());
        CrowdWorkflowResult started = result(run(GraphInput.args(input), config));
        releaseIfCompleted(started, config);
        return started;
    }

    /** Returns whether this session currently owns an interrupted Crowd interaction. */
    public boolean hasPending(UUID sessionId, ChatCaller caller) {
        RunnableConfig config = RunnableConfig.builder().threadId(sessionId.toString()).build();
        var checkpoint = graph.lastStateOf(config);
        if (checkpoint.isEmpty()) {
            return false;
        }
        assertOwner(checkpoint.get().state(), caller);
        return true;
    }

    /** Releases a superseded Crowd interaction without changing its graph node structure. */
    public void cancel(UUID sessionId, ChatCaller caller) {
        RunnableConfig config = RunnableConfig.builder().threadId(sessionId.toString()).build();
        var checkpoint = graph.lastStateOf(config);
        if (checkpoint.isEmpty()) {
            return;
        }
        assertOwner(checkpoint.get().state(), caller);
        try {
            saver.release(config);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to release superseded CROWD_STATUS workflow", exception);
        }
    }

    private void assertOwner(CrowdGraphState state, ChatCaller caller) {
        if (!caller.key().equals(state.value(OWNER, ""))) {
            throw new GlobalException(devkor.com.teamcback.global.response.ResultCode.CHATBOT_SESSION_FORBIDDEN);
        }
    }

    private void releaseIfCompleted(CrowdWorkflowResult result, RunnableConfig config) {
        if (!result.waiting()) {
            try {
                saver.release(config);
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to release completed CROWD_STATUS workflow", exception);
            }
        }
    }

    private CrowdGraphState run(GraphInput input, RunnableConfig config) {
        try {
            var state = graph.invoke(input, config);
            if (state.isPresent()) {
                return state.get();
            }
            return graph.lastStateOf(config).map(snapshot -> snapshot.state())
                    .orElseGet(() -> new CrowdGraphState(Map.of()));
        } catch (Exception exception) {
            throw new IllegalStateException("CROWD_STATUS workflow failed", exception);
        }
    }

    private CrowdWorkflowResult result(CrowdGraphState state) {
        String reply = state.value(REPLY, "");
        boolean waiting = !state.value(AWAITING, "").isBlank()
                || (!state.value(CANDIDATES, List.<Map<String, Object>>of()).isEmpty()
                && state.value(SELECTED_ID, "").isBlank());
        return new CrowdWorkflowResult(true, reply, waiting);
    }

    private Map<String, Object> resolveLocation(CrowdGraphState state) {
        List<ChatbotSearchCandidate> found = searchService.search(state.value(QUERY, ""));
        CrowdTargetResolver.Resolution resolution = targetResolver == null
                ? legacyResolution(found) : targetResolver.resolve(state.value(QUERY, ""), found);
        ChatbotSearchCandidate building = uniqueBuilding(found);
        boolean hasPlaceSearchCandidate = found.stream()
                .anyMatch(candidate -> candidate.locationType() == ToolLocationType.PLACE);
        if (building != null && resolution.status() == CrowdTargetResolver.Resolution.Status.NOT_FOUND
                && !isPartialBuilding(building)) {
            return Map.of(REPLY, building.name() + "에서 확인 가능한 혼잡도 장소가 없어요.",
                    SELECTED_ID, "", AWAITING, "");
        }
        if (building != null && isPartialBuilding(building)
                && !(hasPlaceSearchCandidate && resolution.status() == CrowdTargetResolver.Resolution.Status.UNIQUE)) {
            return Map.of(CANDIDATES, List.of(locationMap(building.locationId(), ToolLocationType.BUILDING,
                            building.name(), "PARTIAL")),
                    SELECTED_ID, String.valueOf(building.locationId()),
                    SELECTED_TYPE, ToolLocationType.BUILDING.name(),
                    SELECTED_NAME, building.name(),
                    BUILDING_ID, String.valueOf(building.locationId()),
                    AWAITING, BUILDING_CONFIRMATION);
        }
        List<Map<String, Object>> candidates = resolution.candidates().stream()
                .limit(5).map(this::crowdMap).toList();
        if (candidates.size() == 1) {
            Map<String, Object> candidate = candidates.get(0);
            return Map.of(CANDIDATES, candidates,
                    SELECTED_ID, String.valueOf(candidate.get("id")),
                    SELECTED_TYPE, candidate.get("type"),
                    SELECTED_NAME, candidate.get("name"),
                    AWAITING, "");
        }
        return Map.of(CANDIDATES, candidates,
                BUILDING_ID, building == null ? "" : String.valueOf(building.locationId()),
                AWAITING, candidates.size() > 1 ? PLACE_SELECTION : "");
    }

    private CrowdTargetResolver.Resolution legacyResolution(List<ChatbotSearchCandidate> found) {
        List<CrowdPlaceCandidate> candidates = found == null ? List.of() : found.stream()
                .filter(candidate -> candidate.locationType() == ToolLocationType.PLACE)
                .map(candidate -> new CrowdPlaceCandidate(candidate.locationId(), candidate.name(), candidate.name(),
                        candidate.floor(), candidate.placeType(), candidate.buildingId(), candidate.buildingName()))
                .toList();
        return candidates.isEmpty() ? CrowdTargetResolver.Resolution.notFoundForCompatibility()
                : CrowdTargetResolver.Resolution.ofForCompatibility(candidates);
    }

    private ChatbotSearchCandidate uniqueBuilding(List<ChatbotSearchCandidate> found) {
        if (found == null) return null;
        List<ChatbotSearchCandidate> buildings = found.stream()
                .filter(candidate -> candidate.locationType() == ToolLocationType.BUILDING).toList();
        return buildings.size() == 1 ? buildings.get(0) : null;
    }

    private boolean isPartialBuilding(ChatbotSearchCandidate candidate) {
        return candidate.sourcePriority() < 300;
    }

    private Map<String, Object> crowdMap(CrowdPlaceCandidate candidate) {
        Map<String, Object> value = locationMap(candidate.placeId(), ToolLocationType.PLACE,
                candidate.deviceName() == null || candidate.deviceName().isBlank()
                        ? candidate.placeName() : candidate.deviceName(), "EXACT");
        value.put("floor", candidate.floor() == null ? "" : String.valueOf(candidate.floor()));
        value.put("placeType", candidate.placeType() == null ? "" : candidate.placeType().name());
        return value;
    }

    private Map<String, Object> locationMap(Long id, ToolLocationType type, String name, String matchType) {
        Map<String, Object> value = new HashMap<>();
        value.put("id", id);
        value.put("type", type.name());
        value.put("name", name);
        value.put("matchType", matchType);
        return value;
    }

    private Map<String, Object> askConfirmation(CrowdGraphState state) {
        List<Map<String, Object>> candidates = state.value(CANDIDATES, List.of());
        if (PLACE_SELECTION.equals(state.value(AWAITING, ""))) {
            String names = candidates.stream().map(item -> String.valueOf(item.get("name"))).distinct()
                    .reduce((left, right) -> left + ", " + right).orElse("");
            return Map.of(REPLY, "혼잡도를 확인할 장소를 하나 골라 주세요: " + names);
        }
        String name = candidates.size() == 1 ? String.valueOf(candidates.get(0).get("name"))
                : candidates.stream().map(item -> String.valueOf(item.get("name"))).distinct()
                .reduce((left, right) -> left + ", " + right).orElse("");
        String reply = candidates.size() == 1
                ? name + "을(를) 말씀하시나요?"
                : "어느 장소의 혼잡도를 확인할까요? " + name;
        return Map.of(REPLY, reply);
    }

    private Map<String, Object> checkConfirmation(CrowdGraphState state) {
        String confirmation = state.value(CONFIRMATION, "").trim();
        if (PLACE_SELECTION.equals(state.value(AWAITING, ""))) {
            if (candidateSelector != null) {
                List<Map<String, Object>> candidates = state.value(CANDIDATES, List.of());
                List<CrowdCandidateView> views = new ArrayList<>();
                for (int index = 0; index < candidates.size(); index++) {
                    Map<String, Object> candidate = candidates.get(index);
                    views.add(new CrowdCandidateView(index, String.valueOf(candidate.get("name")),
                            String.valueOf(candidate.getOrDefault("floor", "")),
                            String.valueOf(candidate.getOrDefault("placeType", ""))));
                }
                CrowdCandidateSelection selection = candidateSelector.select(confirmation, views);
                Integer index = selection == null ? null : selection.candidateIndex();
                if (selection != null && selection.status() == CrowdCandidateSelection.Status.SELECTED
                        && index != null && index >= 0 && index < candidates.size()) {
                    Map<String, Object> candidate = candidates.get(index);
                    return Map.of(SELECTED_ID, String.valueOf(candidate.get("id")),
                            SELECTED_TYPE, candidate.get("type"), SELECTED_NAME, candidate.get("name"),
                            AWAITING, "", REPLY, "", CONFIRMATION, "");
                }
                return Map.of(REPLY, "", SELECTED_ID, "", AWAITING, PLACE_SELECTION, CONFIRMATION, "");
            }
            CrowdTargetResolver.Resolution selection = targetResolver == null
                    ? CrowdTargetResolver.Resolution.notFound()
                    : targetResolver.resolveWithinBuilding(confirmation,
                    parseLong(state.value(BUILDING_ID, "")));
            if (selection.status() == CrowdTargetResolver.Resolution.Status.UNIQUE) {
                Map<String, Object> candidate = crowdMap(selection.candidates().get(0));
                return Map.of(SELECTED_ID, String.valueOf(candidate.get("id")),
                        SELECTED_TYPE, candidate.get("type"), SELECTED_NAME, candidate.get("name"),
                        AWAITING, "", REPLY, "", CONFIRMATION, "");
            }
            return Map.of(REPLY, selection.status() == CrowdTargetResolver.Resolution.Status.AMBIGUOUS
                            ? "후보 중 하나를 더 구체적으로 골라 주세요."
                            : "확인할 장소를 후보 이름으로 알려주세요.",
                    SELECTED_ID, "", AWAITING, PLACE_SELECTION, CONFIRMATION, "");
        }
        if (!isYes(confirmation)) {
            return Map.of(REPLY, "확인할 장소를 다시 알려주세요.", SELECTED_ID, "");
        }
        List<Map<String, Object>> candidates = state.value(CANDIDATES, List.of());
        if (candidates.size() != 1) {
            return Map.of(REPLY, "후보 중 하나를 장소 이름으로 알려주세요.", SELECTED_ID, "");
        }
        Map<String, Object> candidate = candidates.get(0);
        return Map.of(SELECTED_ID, String.valueOf(candidate.get("id")),
                SELECTED_TYPE, candidate.get("type"), SELECTED_NAME, candidate.get("name"),
                REPLY, "", CONFIRMATION, "", AWAITING, "");
    }

    private Map<String, Object> loadCrowd(CrowdGraphState state) {
        Long placeId = Long.valueOf(state.value(SELECTED_ID, "0"));
        GetCrowdStatusToolResult result = campusToolAdapter.getCrowdStatus(
                new GetCrowdStatusToolRequest(placeId, false));
        if (result == null || result.error() != null || result.crowd() == null) {
            return Map.of(REPLY, "해당 장소의 혼잡도 데이터를 확인할 수 없어요.");
        }
        CrowdStatusToolData crowd = result.crowd();
        Map<String, Object> updates = new HashMap<>();
        updates.put(PEOPLE, String.valueOf(crowd.estimatedPeople()));
        updates.put(CAPACITY, String.valueOf(crowd.capacity()));
        updates.put(CROWD_LEVEL, crowd.level() == null ? "" : crowd.level().name());
        updates.put(STALE, String.valueOf(crowd.stale()));
        return updates;
    }

    private Map<String, Object> resolveCrowdTarget(CrowdGraphState state) {
        if (!ToolLocationType.PLACE.name().equals(state.value(SELECTED_TYPE, ""))) {
            CrowdTargetResolver.Resolution resolution = targetResolver == null
                    ? CrowdTargetResolver.Resolution.notFound()
                    : targetResolver.byBuilding(parseLong(state.value(BUILDING_ID, state.value(SELECTED_ID, ""))));
            List<Map<String, Object>> candidates = resolution.candidates().stream()
                    .limit(5).map(this::crowdMap).toList();
            if (candidates.size() == 1) {
                Map<String, Object> candidate = candidates.get(0);
                return Map.of(CANDIDATES, candidates, SELECTED_ID, String.valueOf(candidate.get("id")),
                        SELECTED_TYPE, candidate.get("type"), SELECTED_NAME, candidate.get("name"),
                        AWAITING, "");
            }
            if (candidates.size() > 1) {
                return Map.of(CANDIDATES, candidates, SELECTED_ID, "", AWAITING, PLACE_SELECTION);
            }
            return Map.of(REPLY, state.value(SELECTED_NAME, "장소")
                    + "에서 확인 가능한 혼잡도 장소가 없어요.", SELECTED_ID, "", AWAITING, "");
        }
        return Map.of();
    }

    private Map<String, Object> formatReply(CrowdGraphState state) {
        if (!state.value(REPLY, "").isBlank()) {
            return Map.of();
        }
        String level = state.value(CROWD_LEVEL, "");
        String name = state.value(SELECTED_NAME, "해당 장소");
        return Map.of(REPLY, "현재 " + name + "의 혼잡도는 " + level + "이에요.");
    }

    private String nextAfterResolve(CrowdGraphState state) {
        List<Map<String, Object>> candidates = state.value(CANDIDATES, List.of());
        if (candidates.isEmpty()) return END;
        if (candidates.size() == 1
                && ToolLocationType.PLACE.name().equals(state.value(SELECTED_TYPE, ""))
                && "EXACT".equals(candidates.get(0).get("matchType"))) {
            return TARGET;
        }
        return ASK;
    }

    private String nextAfterConfirmation(CrowdGraphState state) {
        if (state.value(SELECTED_ID, "").isBlank()
                && PLACE_SELECTION.equals(state.value(AWAITING, ""))) return ASK;
        return state.value(SELECTED_ID, "").isBlank() ? END : TARGET;
    }

    private String nextAfterTarget(CrowdGraphState state) {
        if (PLACE_SELECTION.equals(state.value(AWAITING, ""))) return ASK;
        return ToolLocationType.PLACE.name().equals(state.value(SELECTED_TYPE, "")) ? LOAD : FORMAT;
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isYes(String value) {
        return switch (value) {
            case "네", "예", "응", "맞아", "맞아요", "맞습니다", "그래", "그거야" -> true;
            default -> false;
        };
    }

    public record CrowdWorkflowResult(boolean handled, String reply, boolean waiting) {
        public static CrowdWorkflowResult notHandled() {
            return new CrowdWorkflowResult(false, null, false);
        }
    }

    public static final class CrowdGraphState extends AgentState {
        private static Channel<String> text() { return Channels.base(() -> ""); }
        private static Channel<List<Map<String, Object>>> list() { return Channels.base(ArrayList::new); }
        public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
                Map.entry(OWNER, text()), Map.entry(QUERY, text()), Map.entry(CANDIDATES, list()),
                Map.entry(SELECTED_ID, text()), Map.entry(SELECTED_TYPE, text()),
                Map.entry(SELECTED_NAME, text()), Map.entry(REPLY, text()),
                Map.entry(CONFIRMATION, text()), Map.entry(CROWD_LEVEL, text()),
                Map.entry(PEOPLE, text()), Map.entry(CAPACITY, text()), Map.entry(STALE, text()),
                Map.entry(BUILDING_ID, text()), Map.entry(AWAITING, text()));

        public CrowdGraphState(Map<String, Object> data) { super(data); }
    }
}
