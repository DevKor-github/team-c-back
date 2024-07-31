package devkor.com.teamcback.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.common.PlaceType;
import devkor.com.teamcback.domain.facility.entity.Facility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "키워드 검색 결과")
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalSearchRes {
    @Schema(description = "건물 또는 시설 id", example = "1")
    private Long id;
    @Schema(description = "건물 또는 시설 이름", example = "애기능생활관")
    private String name;
    @Schema(description = "강의실 층수", example = "3")
    private Double floor;
    @Schema(description = "건물 주소", example = "서울특별시 성북구 안암로 145 고려대학교 애기능생활관")
    private String address;
    @Schema(description = "설명", example = "프리액션 밸브실")
    private String detail;
    @Schema(description = "건물 또는 시설 경도", example = "127.0274309")
    private Double longitude;
    @Schema(description = "건물 또는 시설 위도", example = "37.5844829")
    private Double latitude;
    @Schema(description = "건물 또는 시설 종류", example = "BUILDING")
    private PlaceType placeType;

    public GlobalSearchRes(Building building, PlaceType placeType) {
        this.id = building.getId();
        this.name = building.getName();
        this.address = building.getAddress();
        this.longitude = building.getNode().getLongitude();
        this.latitude = building.getNode().getLatitude();
        this.placeType = placeType;
    }

    public GlobalSearchRes(Classroom classroom, PlaceType placeType) {
        this.id = classroom.getId();
        if (classroom.getBuilding().getId() == 0) {
            this.name = classroom.getName();
        } else {
            this.name = classroom.getBuilding().getName() + " " + classroom.getName();
        }
        this.floor = classroom.getFloor();
        if (!classroom.getDetail().equals(".")) {
            this.detail = classroom.getDetail();
        }
        this.longitude = classroom.getNode().getLongitude();
        this.latitude = classroom.getNode().getLatitude();
        this.placeType = placeType;
    }

    public GlobalSearchRes(Facility facility, PlaceType placeType) {
        if (facility.getBuilding().getId() == 0 || facility.getType().getName().equals(facility.getName())) {
            this.name = facility.getName();
        } else {
            this.name = facility.getBuilding().getName() + " " + facility.getName();
        }
        this.placeType = placeType;
    }
}
