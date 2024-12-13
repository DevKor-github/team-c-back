package devkor.com.teamcback.domain.routes.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import lombok.Getter;

@Getter
public class Edge extends BaseEntity {
    private long distance;

    private Long startNode;

    private Long endNode;

    public Edge(long distance, Long startNode, Long endNode) {
        this.distance = distance;
        this.startNode = startNode;
        this.endNode = endNode;
    }
}
