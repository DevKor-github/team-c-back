package devkor.com.teamcback.domain.admin.building.dto.response;

import devkor.com.teamcback.domain.building.entity.BuildingImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "건물 내부 사진 조회 응답 dto")
@Getter
public class GetBuildingImageRes {
    private Long buildingImageId;
    private Long buildingId;
    private String buildingName;
    private Double floor;
    private String image;

    public GetBuildingImageRes(BuildingImage buildingImage) {
        this.buildingImageId = buildingImage.getId();
        this.buildingId = buildingImage.getBuilding().getId();
        this.buildingName = buildingImage.getBuilding().getName();
        this.floor = buildingImage.getFloor();
        this.image = buildingImage.getImage();
    }
}
