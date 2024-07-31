package devkor.com.teamcback.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.common.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "건물 및 강의실 조회 결과")
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchPlaceRes {
    @Schema(description = "건물 또는 강의실 id", example = "1")
    private Long id;
    @Schema(description = "건물 id", example = "1")
    private Long buildingId;
    @Schema(description = "건물명", example = "애기능생활관")
    private String buildingName;
    @Schema(description = "건물 또는 강의실 이름", example = "애기능생활관")
    private String name;
    @Schema(description = "건물 또는 강의실 image url", example = "주소")
    private String imageUrl;
    @Schema(description = "건물 또는 강의실 설명", example = "설명")
    private String detail;
    @Schema(description = "강의실 층수", example = "3")
    private Double floor;
    @Schema(description = "건물 주소", example = "서울특별시 성북구 안암로 145 고려대학교 애기능생활관")
    private String address;
    @Schema(description = "건물 운영 시간", example = "00:00~00:00")
    private String operatingTime;
    @Schema(description = "건물 출입 시 학생증 필요 여부", example = "false")
    private Boolean needStudentCard;
    @Schema(description = "강의실 전기 콘센트 유무", example = "true")
    private Boolean plugAvailability;
    @Schema(description = "건물 또는 강의실 경도", example = "127.0274309")
    private Double longitude;
    @Schema(description = "건물 또는 강의실 위도", example = "37.5844829")
    private Double latitude;
    @Schema(description = "강의실 내부지도 상 x좌표", example = "100")
    private Double xCoord;
    @Schema(description = "강의실 내부지도 상 y좌표", example = "250")
    private Double yCoord;
    @Schema(description = "건물 또는 강의실 종류", example = "BUILDING")
    private PlaceType placeType;

    public SearchPlaceRes(Building building) {
        this.id = building.getId();
        this.buildingId = building.getId();
        this.buildingName = building.getName();
        this.name = building.getName();
        this.imageUrl = building.getImageUrl();
        this.detail = building.getDetail();
        this.address = building.getAddress();
        this.operatingTime = building.getOperatingTime();
        this.needStudentCard = building.getNeedStudentCard();
        this.longitude = building.getNode().getLongitude();
        this.latitude = building.getNode().getLatitude();
        this.placeType = PlaceType.BUILDING;
    }

    public SearchPlaceRes(Classroom classroom) {
        this.id = classroom.getId();
        this.buildingId = classroom.getBuilding().getId();
        this.buildingName = classroom.getBuilding().getName();
        this.name = classroom.getName();
        this.imageUrl = classroom.getImageUrl();
        this.detail = classroom.getDetail();
        this.floor = classroom.getFloor();
        this.plugAvailability = classroom.isPlugAvailability();
        this.longitude = classroom.getNode().getLongitude();
        this.latitude = classroom.getNode().getLatitude();
        this.xCoord = classroom.getNode().getXCoord();
        this.yCoord = classroom.getNode().getYCoord();
        this.placeType = PlaceType.CLASSROOM;
    }
}
