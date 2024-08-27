package devkor.com.teamcback.domain.navigate.dto.response;

import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.navigate.entity.Node;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "노드 조회 응답 dto")
@Getter
public class GetNodeDetailRes {
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
    private Long buildingId;
    private String buildingName;
    private String placeType;
    private Long placeId;
    private String placeName;

    public GetNodeDetailRes(Node node) {
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
        this.buildingId = node.getBuilding().getId();
        this.buildingName = node.getBuilding().getName();
    }

//    public void setClassroomPlace(Classroom classroom) {
//        this.placeType = LocationType.CLASSROOM.toString();
//        this.placeId = classroom.getId();
//        this.placeName = classroom.getName();
//    }

    public void setFacilityPlace(Place place) {
        this.placeType = LocationType.PLACE.toString();
        this.placeId = place.getId();
        this.placeName = place.getName();
    }
}
