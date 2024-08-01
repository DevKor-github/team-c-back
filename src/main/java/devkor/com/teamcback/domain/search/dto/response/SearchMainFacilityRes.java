package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class SearchMainFacilityRes {
    @Schema(description = "편의시설명", example = "2층 학생식당")
    private String name;
    @Schema(description = "편의시설 종류", example = "CAFETERIA")
    private FacilityType type;
    @Schema(description = "편의시설 id", example = "1")
    private Long placeId;
    @Schema(description = "이미지 URL")
    private String imageUrl;

    public SearchMainFacilityRes(Facility facility) {
        if(facility.getFloor() > 0) {
            int floor = (int) Math.floor(facility.getFloor());
            this.name = floor + "층 " + facility.getName();
        } else {
            int floor = (int) Math.floor(facility.getFloor() * -1);
            this.name = "B" + floor + "층 " + facility.getName();
        }

        this.type = facility.getType();
        this.placeId = facility.getId();
        this.imageUrl = facility.getImageUrl();
    }
}
