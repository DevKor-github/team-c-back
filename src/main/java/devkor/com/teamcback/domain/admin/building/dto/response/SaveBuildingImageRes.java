package devkor.com.teamcback.domain.admin.building.dto.response;

import devkor.com.teamcback.domain.building.entity.BuildingImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "건물 내부 사진 저장 완료")
@Getter
public class SaveBuildingImageRes {
    private Long buildingImageId;
    private String imageUrl;

    public SaveBuildingImageRes(BuildingImage buildingImage) {
        this.buildingImageId = buildingImage.getId();
        this.imageUrl = buildingImage.getImage();
    }
}
