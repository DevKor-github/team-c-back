package devkor.com.teamcback.domain.routes.dto.response;

import devkor.com.teamcback.domain.routes.entity.Edge;
import devkor.com.teamcback.domain.routes.entity.Node;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetGraphRes {
    private List<Node> graphNode;
    private Map<Long, List<Edge>> graphEdge;

    public GetGraphRes(List<Node> graphNode, Map<Long, List<Edge>> graphEdge){
        this.graphNode = graphNode;
        this.graphEdge = graphEdge;
    }
}
