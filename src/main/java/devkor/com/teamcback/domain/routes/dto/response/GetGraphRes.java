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
    private Map<Long, List<Edge>> graphEdge;
    private Map<Long, Node> nodeMap;

    public GetGraphRes(Map<Long, List<Edge>> graphEdge, Map<Long, Node> nodeMap) {
        this.graphEdge = graphEdge;
        this.nodeMap = nodeMap;
    }
}
