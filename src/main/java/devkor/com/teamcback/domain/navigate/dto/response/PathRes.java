package devkor.com.teamcback.domain.navigate.dto.response;

import devkor.com.teamcback.domain.navigate.entity.Node;
import java.util.Objects;
import lombok.Getter;

@Getter
public class PathRes {
    private Long nodeId;
    private double xCoord;
    private double yCoord;
    private int floor;

    public PathRes(Node node) {
        this.nodeId = node.getId();
        this.xCoord = node.getXCoord();
        this.yCoord = node.getYCoord();
        this.floor = node.getFloor();
    }
}
