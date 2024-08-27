package devkor.com.teamcback.domain.routes.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import lombok.Getter;

@Getter
public class Edge extends BaseEntity {
    private int distance;

    private Long startNode;

    private Long endNode;

    public Edge(int distance, Long startNode, Long endNode) {
        this.distance = distance;
        this.startNode = startNode;
        this.endNode = endNode;
    }
}
