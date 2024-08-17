package devkor.com.teamcback.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.facility.entity.Facility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "편의시설 정보")
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchFacilityRes {
    @Schema(description = "편의시설 ID", example = "1")
    private Long facilityId;
    @Schema(description = "편의시설 이름", example = "쓰레기통1")
    private String name;
    @Schema(description = "편의시설 이용 가능 여부", example = "true")
    private Boolean availability;
    @Schema(description = "운영 시간", example = "08:30-17:00")
    private String operatingTime;
    @Schema(description = "운영 여부", example = "true")
    private boolean isOperating;
    @Schema(description = "편의시설 image url", example = "url")
    private String imageUrl;
    @Schema(description = "편의시설 내부지도 상 x좌표", example = "100")
    private Double xCoord;
    @Schema(description = "편의시설 내부지도 상 y좌표", example = "250")
    private Double yCoord;
    @Schema(description = "편의시설 경도", example = "127.0274309")
    private Double longitude;
    @Schema(description = "편의시설 위도", example = "37.5844829")
    private Double latitude;
    @Schema(description = "편의시설 설명", example = "남자화장실, 여자화장실")
    private String detail;
    @Schema(description = "편의시설 층", example = "1")
    private int floor;

    public SearchFacilityRes(Facility facility) {
        this.facilityId = facility.getId();
        this.name = facility.getName();
        this.availability = facility.isAvailability();
        this.operatingTime = facility.getOperatingTime();
        this.isOperating = facility.isOperating();
        this.imageUrl = facility.getImageUrl();
        this.xCoord = facility.getNode().getXCoord();
        this.yCoord = facility.getNode().getYCoord();
        this.longitude = facility.getNode().getLongitude();
        this.latitude = facility.getNode().getLatitude();
        this.detail = facility.getDetail();
        this.floor = facility.getFloor().intValue();
    }
}
