package devkor.com.teamcback.domain.search.entity;

import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.search.dto.request.SaveSearchLogReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SearchLog {
    private Long id;
    private String name;
    private LocationType locationType;
    private Double floor;
    private String address;
    private String detail;
    private Double longitude;
    private Double latitude;
    private PlaceType placeType;
    private Long buildingId;
    private String searchedAt;

    public SearchLog(SaveSearchLogReq req, String searchedAt) {
        this.id = req.getId();
        this.name = req.getName();
        this.locationType = req.getLocationType();
        this.floor = req.getFloor();
        this.address = req.getAddress();
        this.detail = req.getDetail();
        this.longitude = req.getLongitude();
        this.latitude = req.getLatitude();
        this.placeType = req.getPlaceType();
        this.buildingId = req.getBuildingId();
        this.searchedAt = searchedAt;
    }
}
