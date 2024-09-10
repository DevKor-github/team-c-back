package devkor.com.teamcback.domain.routes.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import lombok.Getter;

@Getter
public class Edge extends BaseEntity {
    private int distance;

    private Node startNode;

    private Node endNode;

    public Edge(int distance, Node startNode, Node endNode) {
        this.distance = distance;
        this.startNode = startNode;
        this.endNode = endNode;
    }
}
