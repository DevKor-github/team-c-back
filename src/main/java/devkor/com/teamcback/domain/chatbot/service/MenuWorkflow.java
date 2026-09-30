package devkor.com.teamcback.domain.chatbot.service;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import devkor.com.teamcback.domain.chatbot.dto.*;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.*;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.time.LocalDate;
import java.util.*;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.action.*;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import devkor.com.teamcback.domain.chatbot.checkpoint.RedisCheckpointSaver;
import org.bsc.langgraph4j.state.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="chatbot", name="enabled", havingValue="true")
public class MenuWorkflow {
    private static final String SEARCH="search", DECIDE="decide", ASK="ask", SELECT="select", LOAD="load", FORMAT="format";
    private static final String OWNER="owner", QUERY="query", DATE="date", INPUT="input", CANDIDATES="candidates", ID="id", NAME="name", REPLY="reply", WAITING="waiting", DATA="data";
    private final MenuPlanner planner; private final CampusToolAdapter adapter; private final LocationCandidateSelector selector;
    private final BaseCheckpointSaver saver; private final CompiledGraph<State> graph;
    @Autowired public MenuWorkflow(MenuPlanner planner, CampusToolAdapter adapter, LocationCandidateSelector selector, BaseCheckpointSaver saver) {
        this.planner=planner; this.adapter=adapter; this.selector=selector; this.saver=saver;
        try { StateGraph<State> s=new StateGraph<>(State.SCHEMA, State::new);
            s.addNode(SEARCH, AsyncNodeAction.node_async((NodeAction<State>)this::search)); s.addNode(DECIDE,AsyncNodeAction.node_async((NodeAction<State>)this::decide));
            s.addNode(ASK,AsyncNodeAction.node_async((NodeAction<State>)this::ask)); s.addNode(SELECT,AsyncNodeAction.node_async((NodeAction<State>)this::select));
            s.addNode(LOAD,AsyncNodeAction.node_async((NodeAction<State>)this::load)); s.addNode(FORMAT,AsyncNodeAction.node_async((NodeAction<State>)this::format));
            s.addEdge(START,SEARCH); s.addEdge(SEARCH,DECIDE); s.addConditionalEdges(DECIDE,AsyncEdgeAction.edge_async((EdgeAction<State>)this::afterDecision),Map.of(ASK,ASK,LOAD,LOAD,END,END));
            s.addEdge(ASK,SELECT); s.addConditionalEdges(SELECT,AsyncEdgeAction.edge_async((EdgeAction<State>)this::afterSelect),Map.of(ASK,ASK,LOAD,LOAD)); s.addEdge(LOAD,FORMAT); s.addEdge(FORMAT,END);
            graph=s.compile(CompileConfig.builder().checkpointSaver(saver).interruptAfter(ASK).releaseThread(true).build());
        } catch(GraphStateException e){throw new IllegalStateException("Unable to compile MENU workflow",e);}
    }
    public MenuWorkflow(MenuPlanner planner, CampusToolAdapter adapter, LocationCandidateSelector selector) {
        this(planner, adapter, selector, new MemorySaver());
    }

    public WorkflowResult handle(UUID sessionId, ChatCaller caller,List<LlmGateway.ConversationMessage> history,String message){
        RunnableConfig c=config(sessionId); var checkpoint=graph.lastStateOf(c); State state;
        if(checkpoint.isPresent()){assertOwner(checkpoint.get().state(),caller); state=run(GraphInput.resume(Map.of(INPUT,message)),c);}
        else {MenuPlan p=planner.plan(history,message); if(p==null||!p.isMenu())return WorkflowResult.notHandled(); state=run(GraphInput.args(Map.of(OWNER,caller.key(),QUERY,p.cafeteriaQuery(),DATE,p.dateExpression()==null?"":p.dateExpression(),INPUT,message)),c);}
        WorkflowResult result=new WorkflowResult(true,state.value(REPLY,""),!state.value(WAITING,"").isBlank()); if(!result.waiting())release(c); return result;
    }
    public boolean hasPending(UUID id,ChatCaller caller){var x=graph.lastStateOf(config(id));if(x.isEmpty())return false;assertOwner(x.get().state(),caller);return true;}
    public void cancel(UUID id,ChatCaller caller){var x=graph.lastStateOf(config(id));if(x.isPresent()){assertOwner(x.get().state(),caller);release(config(id));}}
    private Map<String,Object> search(State st){SearchCampusToolResult r=adapter.searchCampus(new SearchCampusToolRequest(st.value(QUERY,""),5));List<Map<String,Object>> out=new ArrayList<>();if(r!=null&&r.candidates()!=null)for(SearchCampusItem i:r.candidates())if(i.locationType()==ToolLocationType.PLACE&&i.placeType()==devkor.com.teamcback.domain.place.entity.PlaceType.CAFETERIA)out.add(Map.of(ID,String.valueOf(i.locationId()),NAME,i.name(),"buildingName",Objects.toString(i.buildingName(),""),"floor",Objects.toString(i.floor(),"")));
        return Map.of(CANDIDATES,out,REPLY,out.isEmpty()?"요청한 식당을 찾지 못했어요.":"");}
    private Map<String,Object> decide(State st){List<Map<String,Object>> c=st.value(CANDIDATES,List.of());if(c.size()==1)return selected(c.get(0));if(c.size()>1)return Map.of(WAITING,"selection");return Map.of(WAITING,"");}
    private Map<String,Object> ask(State st){String names=names(st);return Map.of(REPLY,"어느 식당의 메뉴인지 선택해 주세요: "+names);}
    private Map<String,Object> select(State st){List<Map<String,Object>> cs=st.value(CANDIDATES,List.of());List<LocationCandidateView> views=new ArrayList<>();for(int i=0;i<cs.size();i++){Map<String,Object> c=cs.get(i);views.add(new LocationCandidateView(i,String.valueOf(c.get(NAME)),ToolLocationType.PLACE,String.valueOf(c.get("buildingName")),String.valueOf(c.get("floor")),"CAFETERIA"));}LocationCandidateSelection x=selector.select(st.value(INPUT,""),views);Integer i=x==null?null:x.candidateIndex();if(x!=null&&x.status()==CandidateSelection.Status.SELECTED&&i!=null&&i>=0&&i<cs.size())return selected(cs.get(i));return Map.of(WAITING,"selection",INPUT,"",REPLY,"후보 중 식당 이름을 정확히 골라 주세요: "+names(st));}
    private Map<String,Object> load(State st){try{LocalDate d=WorkflowDates.menuDate(st.value(DATE,""));GetCafeteriaMenuToolResult r=adapter.getCafeteriaMenu(new GetCafeteriaMenuToolRequest(Long.valueOf(st.value(ID,"0")),d,d));return Map.of(DATA,menuText(r),WAITING,"");}catch(IllegalArgumentException e){return Map.of(REPLY,"조회할 수 없는 날짜예요.",WAITING,"");}}
    private Map<String,Object> format(State st){return Map.of(REPLY,st.value(DATA,""));}
    private String menuText(GetCafeteriaMenuToolResult r){if(r==null||r.error()!=null)return r==null?"메뉴 정보를 확인하지 못했어요.":r.error().message();StringBuilder b=new StringBuilder(r.cafeteria().placeName());for(CafeteriaMenuDay day:r.cafeteria().days()){b.append("\n").append(day.date()).append(": ");b.append(day.meals().stream().map(m->m.mealType()+" "+m.menu()).reduce((a,x)->a+" / "+x).orElse("등록된 메뉴가 없어요"));}return b.toString();}
    private String afterDecision(State s){return s.value(CANDIDATES,List.<Map<String,Object>>of()).isEmpty()?END:(!s.value(ID,"").isBlank()?LOAD:ASK);} private String afterSelect(State s){return s.value(ID,"").isBlank()?ASK:LOAD;}
    private Map<String,Object> selected(Map<String,Object> c){return Map.of(ID,c.get(ID),NAME,c.get(NAME),WAITING,"");} private String names(State s){return s.value(CANDIDATES,List.<Map<String,Object>>of()).stream().map(c->String.valueOf(c.get(NAME))).distinct().reduce((a,b)->a+", "+b).orElse("");}
    private State run(GraphInput i,RunnableConfig c){try{return graph.invoke(i,c).orElseGet(()->graph.lastStateOf(c).orElseThrow().state());}catch(Exception e){throw new IllegalStateException("MENU workflow failed",e);}} private RunnableConfig config(UUID id){return RunnableConfig.builder().threadId("menu:"+id).build();} private void release(RunnableConfig c){try{saver.release(c);}catch(Exception e){throw new IllegalStateException(e);}} private void assertOwner(State s,ChatCaller c){if(!c.key().equals(s.value(OWNER,"")))throw new GlobalException(devkor.com.teamcback.global.response.ResultCode.CHATBOT_SESSION_FORBIDDEN);}
    public record WorkflowResult(boolean handled,String reply,boolean waiting){static WorkflowResult notHandled(){return new WorkflowResult(false,null,false);}}
    public static final class State extends AgentState {static Channel<String> text(){return Channels.base(()->"");}static Channel<List<Map<String,Object>>> list(){return Channels.base(ArrayList::new);}static Channel<Object> obj(){return Channels.base(()->"");}static final Map<String,Channel<?>> SCHEMA=Map.of(OWNER,text(),QUERY,text(),DATE,text(),INPUT,text(),ID,text(),NAME,text(),REPLY,text(),WAITING,text(),CANDIDATES,list(),DATA,obj());public State(Map<String,Object>d){super(d);}}
}
