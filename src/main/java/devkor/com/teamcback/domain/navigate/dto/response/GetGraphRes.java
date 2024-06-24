package devkor.com.teamcback.domain.navigate.dto.response;

import devkor.com.teamcback.domain.navigate.entity.Edge;
import devkor.com.teamcback.domain.navigate.entity.Node;
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
