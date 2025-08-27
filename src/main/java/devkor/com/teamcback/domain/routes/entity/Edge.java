package devkor.com.teamcback.domain.routes.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Edge extends BaseEntity {
    private long distance;

    private Long startNode;

    private Long endNode;

    @Setter
    private long weight;

    public Edge(long distance, long weight, Long startNode, Long endNode) {
        this.distance = distance;
        this.weight = weight;
        this.startNode = startNode;
        this.endNode = endNode;
    }

}
