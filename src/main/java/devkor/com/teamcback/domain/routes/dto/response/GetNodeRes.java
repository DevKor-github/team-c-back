package devkor.com.teamcback.domain.routes.dto.response;

import devkor.com.teamcback.domain.routes.entity.Node;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "노드 응답 dto")
@Getter
public class GetNodeRes {
    private Long nodeId;
    private String nodeType;
    private Double xCoord;
    private Double yCoord;
    private Double latitude;
    private Double longitude;
    private Double floor;
    private boolean routing;
    private String adjacentNode;
    private String distance;

    public GetNodeRes(Node node) {
        this.nodeId = node.getId();
        this.nodeType = node.getType().toString();
        this.xCoord = node.getXCoord();
        this.yCoord = node.getYCoord();
        this.latitude = node.getLatitude();
        this.longitude = node.getLongitude();
        this.floor = node.getFloor();
        this.routing = node.isRouting();
        this.adjacentNode = node.getAdjacentNode();
        this.distance = node.getDistance();
    }
}
