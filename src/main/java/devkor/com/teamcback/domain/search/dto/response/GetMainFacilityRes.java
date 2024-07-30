package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import lombok.Getter;

@Getter
public class GetMainFacilityRes {
    private String name;
    private FacilityType type;
    private Long placeId;
    private String imageUrl;

    public GetMainFacilityRes(Facility facility) {
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
