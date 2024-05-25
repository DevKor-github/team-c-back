package devkor.com.teamcback.domain.navigate.dto.response;

import devkor.com.teamcback.domain.navigate.entity.Node;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InnerRouteRes {
    private Long startNodeId;
    private String startNodeName;
    private Long endNodeId;
    private String endNodeName;
    private Integer duration;
    private List<PathRes> path; // 노드 ID 리스트

    public InnerRouteRes(Node startNode, Node endNode, Integer duration, List<PathRes> path) {
        this.startNodeId = startNode.getId();
        this.endNodeId = endNode.getId();
        this.duration = duration;
        this.path = path;
    }
}
