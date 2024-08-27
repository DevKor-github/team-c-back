package devkor.com.teamcback.domain.routes.dto.response;

import devkor.com.teamcback.domain.routes.entity.Edge;
import devkor.com.teamcback.domain.routes.entity.Node;
import java.util.List;
import lombok.Getter;

@Getter
public class GetGraphRes {
    private List<Node> graphNode;
    private List<Edge> graphEdge;

    public GetGraphRes(List<Node> graphNode, List<Edge> graphEdge){
        this.graphNode = graphNode;
        this.graphEdge = graphEdge;
    }
}
