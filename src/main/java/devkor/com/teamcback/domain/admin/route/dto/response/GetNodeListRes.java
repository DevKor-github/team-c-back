package devkor.com.teamcback.domain.admin.route.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Schema(description = "노드 리스트 응답 dto")
@Getter
public class GetNodeListRes {
    private Long buildingId;
    private String buildingName;
    private String buildingImage;
    private Double floor;
    private List<GetNodeRes> nodeList;

    public GetNodeListRes(Building building, Double floor, BuildingImage buildingImage, List<GetNodeRes> nodeList) {
        this.buildingId = building.getId();
        this.buildingName = building.getName();
        this.buildingImage = buildingImage.getImage();
        this.floor = floor;
        this.nodeList = nodeList;
    }
}
