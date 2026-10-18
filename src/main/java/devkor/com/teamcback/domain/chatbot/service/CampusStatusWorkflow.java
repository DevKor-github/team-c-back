package devkor.com.teamcback.domain.chatbot.service;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

import devkor.com.teamcback.domain.chatbot.tool.CampusToolAdapter;
import devkor.com.teamcback.domain.chatbot.tool.dto.CampusStatusToolResult;
import devkor.com.teamcback.domain.chatbot.gateway.LlmGateway;
import java.util.Map;
import java.util.UUID;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Small deterministic graph for current academic/campus status. */
@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class CampusStatusWorkflow {
    private static final String LOAD="get_campus_status", FORMAT="format_response", REPLY="reply";
    private final CampusToolAdapter adapter;
    private final CompiledGraph<StatusGraphState> graph;

    @Autowired
    public CampusStatusWorkflow(CampusToolAdapter adapter) {
        this.adapter=adapter;
        try { StateGraph<StatusGraphState> s=new StateGraph<>(StatusGraphState.SCHEMA,StatusGraphState::new);
            s.addNode(LOAD, AsyncNodeAction.node_async((NodeAction<StatusGraphState>)this::load));
            s.addNode(FORMAT, AsyncNodeAction.node_async((NodeAction<StatusGraphState>)this::format)); s.addEdge(START,LOAD);s.addEdge(LOAD,FORMAT);s.addEdge(FORMAT,END);graph=s.compile();
        } catch(GraphStateException e){throw new IllegalStateException("Unable to compile CAMPUS_STATUS workflow",e);}
    }
    public WorkflowResult handle(UUID sessionId, ChatCaller caller, String message) { try { var output=graph.invoke(GraphInput.args(Map.of()), org.bsc.langgraph4j.RunnableConfig.builder().threadId("campus-status:"+sessionId).build()); StatusGraphState state=output.orElseThrow(); return new WorkflowResult(true,state.value(REPLY,"")); } catch(Exception e){throw new IllegalStateException("CAMPUS_STATUS workflow failed",e);} }
    private Map<String,Object> load(StatusGraphState s){CampusStatusToolResult r=adapter.getCampusStatus();return Map.of("term",r==null?"":String.valueOf(r.term()),"vacation",r!=null&&r.vacation(),"koyeon",r!=null&&r.koyeonPeriod(),"error",r!=null&&r.error()!=null?r.error().message():"");}
    private Map<String,Object> format(StatusGraphState s){String error=s.value("error","");if(!error.isBlank())return Map.of(REPLY,error);StringBuilder b=new StringBuilder();b.append(s.value("vacation",false)?"현재 방학 기간이에요.":"현재 학기 중이에요.");if(!s.value("term","").isBlank())b.append("\n학기: ").append(s.value("term",""));if(s.value("koyeon",false))b.append("\n고연전 기간이에요.");return Map.of(REPLY,b.toString());}
    public record WorkflowResult(boolean handled,String reply){}
    public static final class StatusGraphState extends AgentState {private static Channel<String> text(){return Channels.base(()->"");}private static Channel<Boolean> bool(){return Channels.base(()->false);}public static final Map<String,Channel<?>> SCHEMA=Map.of("term",text(),"error",text(),REPLY,text(),"vacation",bool(),"koyeon",bool());public StatusGraphState(Map<String,Object>d){super(d);}}
}
