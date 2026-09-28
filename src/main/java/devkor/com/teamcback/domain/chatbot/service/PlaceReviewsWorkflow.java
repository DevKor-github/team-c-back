package devkor.com.teamcback.domain.chatbot.service;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

import devkor.com.teamcback.domain.chatbot.dto.LocationCandidateSelection;
import devkor.com.teamcback.domain.chatbot.dto.LocationCandidateView;
import devkor.com.teamcback.domain.chatbot.dto.PlaceReviewsPlan;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetPlaceReviewsToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetPlaceReviewsToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.PlaceReviewsToolData;
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
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Backend-owned reviews workflow. Place IDs never leave graph state. */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class PlaceReviewsWorkflow {
    private static final String SEARCH="search_location", DECIDE="candidate_decision", ASK="ask_clarification",
            SELECT="select_candidate", LOAD="get_reviews", FORMAT="format_response";
    private static final String OWNER="owner", QUERY="query", CANDIDATES="candidates", ID="selectedId",
            NAME="selectedName", REPLY="reply", INPUT="input", AWAITING="awaiting", REVIEWS="reviews";
    private final PlaceReviewsPlanner planner;
    private final CampusToolAdapter adapter;
    private final LocationCandidateSelector selector;
    private final MemorySaver saver = new MemorySaver();
    private final CompiledGraph<ReviewGraphState> graph;

    @Autowired
    public PlaceReviewsWorkflow(PlaceReviewsPlanner planner, CampusToolAdapter adapter,
                                LocationCandidateSelector selector) {
        this.planner=planner; this.adapter=adapter; this.selector=selector;
        try {
            StateGraph<ReviewGraphState> s = new StateGraph<>(ReviewGraphState.SCHEMA, ReviewGraphState::new);
            s.addNode(SEARCH, AsyncNodeAction.node_async((NodeAction<ReviewGraphState>) this::search));
            s.addNode(DECIDE, AsyncNodeAction.node_async((NodeAction<ReviewGraphState>) this::decide));
            s.addNode(ASK, AsyncNodeAction.node_async((NodeAction<ReviewGraphState>) this::ask));
            s.addNode(SELECT, AsyncNodeAction.node_async((NodeAction<ReviewGraphState>) this::select));
            s.addNode(LOAD, AsyncNodeAction.node_async((NodeAction<ReviewGraphState>) this::load));
            s.addNode(FORMAT, AsyncNodeAction.node_async((NodeAction<ReviewGraphState>) this::format));
            s.addEdge(START, SEARCH); s.addEdge(SEARCH, DECIDE);
            s.addConditionalEdges(DECIDE, AsyncEdgeAction.edge_async((EdgeAction<ReviewGraphState>) this::afterDecision),
                    Map.of(ASK,ASK,LOAD,LOAD,END,END));
            s.addEdge(ASK, SELECT);
            s.addConditionalEdges(SELECT, AsyncEdgeAction.edge_async((EdgeAction<ReviewGraphState>) this::afterSelection),
                    Map.of(ASK,ASK,LOAD,LOAD));
            s.addEdge(LOAD, FORMAT); s.addEdge(FORMAT, END);
            graph=s.compile(CompileConfig.builder().checkpointSaver(saver).interruptAfter(ASK).releaseThread(true).build());
        } catch (GraphStateException e) { throw new IllegalStateException("Unable to compile REVIEW workflow", e); }
    }

    public WorkflowResult handle(UUID sessionId, ChatCaller caller, List<LlmGateway.ConversationMessage> history,
                                 String message) {
        RunnableConfig config=config(sessionId); var checkpoint=graph.lastStateOf(config); ReviewGraphState state;
        if (checkpoint.isPresent()) { assertOwner(checkpoint.get().state(),caller); state=run(GraphInput.resume(Map.of(INPUT,message)),config); }
        else { PlaceReviewsPlan plan=planner.plan(history,message); if(plan==null||!plan.isPlaceReviews()) return WorkflowResult.notHandled();
            state=run(GraphInput.args(Map.of(OWNER,caller.key(),QUERY,plan.locationQuery(),INPUT,message)),config); }
        WorkflowResult result=result(state); if(!result.waiting()) release(config); return result;
    }
    public boolean hasPending(UUID sessionId, ChatCaller caller) { var c=graph.lastStateOf(config(sessionId)); if(c.isEmpty())return false; assertOwner(c.get().state(),caller); return true; }
    public void cancel(UUID sessionId, ChatCaller caller) { var c=graph.lastStateOf(config(sessionId)); if(c.isEmpty())return; assertOwner(c.get().state(),caller); release(config(sessionId)); }

    private Map<String,Object> search(ReviewGraphState state) {
        SearchCampusToolResult found=adapter.searchCampus(new SearchCampusToolRequest(state.value(QUERY,""),5));
        List<Map<String,Object>> values=new ArrayList<>();
        if(found!=null&&found.candidates()!=null) for(SearchCampusItem item:found.candidates()) if(item.locationType()==ToolLocationType.PLACE) {
            Map<String,Object> v=new HashMap<>(); v.put("id",item.locationId()); v.put("name",item.name()); v.put("buildingName",item.buildingName());
            v.put("type",item.locationType().name()); v.put("floor",item.floor()==null?"":String.valueOf(item.floor()));
            v.put("placeType",item.placeType()==null?"":item.placeType().name()); values.add(v);
        }
        return Map.of(CANDIDATES,values,REPLY,values.isEmpty()?"리뷰를 확인할 수 있는 장소를 찾지 못했어요.":"");
    }
    private Map<String,Object> decide(ReviewGraphState state) { List<Map<String,Object>> c=state.value(CANDIDATES,List.of()); if(c.size()==1)return selected(c.get(0)); if(c.size()>1)return Map.of(AWAITING,"LOCATION_SELECTION"); return Map.of(AWAITING,""); }
    private Map<String,Object> ask(ReviewGraphState state) { String n=state.value(CANDIDATES,List.<Map<String,Object>>of()).stream().map(c->String.valueOf(c.get("name"))).distinct().reduce((a,b)->a+", "+b).orElse(""); return Map.of(REPLY,"어느 장소의 리뷰를 볼까요? "+n); }
    private Map<String,Object> select(ReviewGraphState state) {
        List<Map<String,Object>> c=state.value(CANDIDATES,List.of()); List<LocationCandidateView> v=new ArrayList<>();
        for(int i=0;i<c.size();i++){Map<String,Object>x=c.get(i);v.add(new LocationCandidateView(i,String.valueOf(x.get("name")),ToolLocationType.PLACE,String.valueOf(x.get("buildingName")),String.valueOf(x.get("floor")),String.valueOf(x.get("placeType"))));}
        LocationCandidateSelection s=selector.select(state.value(INPUT,""),v); Integer i=s==null?null:s.candidateIndex();
        if(s!=null&&s.status()==LocationCandidateSelection.Status.SELECTED&&i!=null&&i>=0&&i<c.size())return selected(c.get(i));
        return Map.of(AWAITING,"LOCATION_SELECTION",INPUT,"",REPLY,"후보 중 하나를 장소 이름으로 말씀해 주세요.");
    }
    private Map<String,Object> load(ReviewGraphState state) { GetPlaceReviewsToolResult r=adapter.getPlaceReviews(new GetPlaceReviewsToolRequest(Long.valueOf(state.value(ID,"0")),5)); if(r==null||r.reviews()==null)return Map.of(REPLY,r!=null&&r.error()!=null?r.error().message():"리뷰 데이터가 없어요."); PlaceReviewsToolData d=r.reviews(); Map<String,Object> m=new HashMap<>();m.put("name",d.placeName());m.put("rating",d.averageRating());m.put("tags",d.topTags());m.put("reviews",d.reviews());return Map.of(REVIEWS,m); }
    private Map<String,Object> format(ReviewGraphState state) { if(!state.value(REPLY,"").isBlank())return Map.of(); Map<String,Object>d=state.value(REVIEWS,Map.of()); StringBuilder b=new StringBuilder(String.valueOf(d.getOrDefault("name",state.value(NAME,"장소")))); if(d.get("rating")!=null)b.append(" 평균 평점 ").append(d.get("rating")); Object tags=d.get("tags");if(tags!=null&&!String.valueOf(tags).equals("[]"))b.append("\n주요 태그: ").append(tags); Object reviews=d.get("reviews");if(reviews!=null&&!String.valueOf(reviews).equals("[]"))b.append("\n후기: ").append(reviews);return Map.of(REPLY,b.toString()); }
    private Map<String,Object> selected(Map<String,Object> c){return Map.of(ID,String.valueOf(c.get("id")),NAME,c.get("name"),AWAITING,"",INPUT,"");}
    private String afterDecision(ReviewGraphState s){if(s.value(CANDIDATES,List.<Map<String,Object>>of()).isEmpty())return END;return s.value(ID,"").isBlank()?ASK:LOAD;}
    private String afterSelection(ReviewGraphState s){return s.value(ID,"").isBlank()?ASK:LOAD;}
    private WorkflowResult result(ReviewGraphState s){return new WorkflowResult(true,s.value(REPLY,""),!s.value(AWAITING,"").isBlank());}
    private ReviewGraphState run(GraphInput i,RunnableConfig c){try{return graph.invoke(i,c).orElseGet(()->graph.lastStateOf(c).orElseThrow().state());}catch(Exception e){throw new IllegalStateException("REVIEW workflow failed",e);}}
    private RunnableConfig config(UUID id){return RunnableConfig.builder().threadId("review:"+id).build();}
    private void release(RunnableConfig c){try{saver.release(c);}catch(Exception e){throw new IllegalStateException(e);}}
    private void assertOwner(ReviewGraphState s,ChatCaller c){if(!c.key().equals(s.value(OWNER,"")))throw new GlobalException(devkor.com.teamcback.global.response.ResultCode.CHATBOT_SESSION_FORBIDDEN);}
    public record WorkflowResult(boolean handled,String reply,boolean waiting){public static WorkflowResult notHandled(){return new WorkflowResult(false,null,false);}}
    public static final class ReviewGraphState extends AgentState { private static Channel<String> text(){return Channels.base(()->"");} private static Channel<List<Map<String,Object>>> list(){return Channels.base(ArrayList::new);} private static Channel<Map<String,Object>> map(){return Channels.<Map<String,Object>>base(() -> new HashMap<>());} public static final Map<String,Channel<?>> SCHEMA=Map.ofEntries(Map.entry(OWNER,text()),Map.entry(QUERY,text()),Map.entry(CANDIDATES,list()),Map.entry(ID,text()),Map.entry(NAME,text()),Map.entry(REPLY,text()),Map.entry(INPUT,text()),Map.entry(AWAITING,text()),Map.entry(REVIEWS,map())); public ReviewGraphState(Map<String,Object>d){super(d);} }
}
