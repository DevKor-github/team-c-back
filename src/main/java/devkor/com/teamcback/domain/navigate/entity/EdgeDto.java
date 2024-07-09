package devkor.com.teamcback.domain.navigate.entity;

import lombok.Getter;

@Getter
public class EdgeDto {

    private int distance;

    private Long startNode;

    private Long endNode;

    public EdgeDto(int distance, Long startNode, Long endNode) {
        this.distance = distance;
        this.startNode = startNode;
        this.endNode = endNode;
    }
}
