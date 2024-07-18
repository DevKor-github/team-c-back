package devkor.com.teamcback.domain.navigate.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingImage;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class GetNodeListRes {
    private Long buildingId;
    private String buildingName;
    private String buildingImage;
    private Double floor;
    private List<GetNodeRes> nodeList = new ArrayList<>();

    public GetNodeListRes(Building building, Double floor, BuildingImage buildingImage, List<GetNodeRes> nodeList) {
        this.buildingId = building.getId();
        this.buildingName = building.getName();
        this.buildingImage = buildingImage.getImage();
        this.floor = floor;
        this.nodeList = nodeList;
    }
}
