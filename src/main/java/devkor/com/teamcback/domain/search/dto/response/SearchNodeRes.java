package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.routes.entity.Node;
import devkor.com.teamcback.domain.routes.entity.NodeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

@Getter
public class SearchNodeRes {
    private Long id;
    @Enumerated(EnumType.STRING)
    private NodeType type;
    private Double xCoord;
    private Double yCoord;
    private Double latitude;
    private Double longitude;

    public SearchNodeRes(Node node) {
        this.id = node.getId();
        this.type = node.getType();
        this.xCoord = node.getXCoord();
        this.yCoord = node.getYCoord();
        this.latitude = node.getLatitude();
        this.longitude = node.getLongitude();
    }
}
