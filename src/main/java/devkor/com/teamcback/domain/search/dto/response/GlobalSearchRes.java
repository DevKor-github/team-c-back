package devkor.com.teamcback.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.entity.Color;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "키워드 검색 결과")
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalSearchRes {
    @JsonInclude(JsonInclude.Include.ALWAYS)
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
    private LocationType locationType;
    @Schema(description = "편의시설 종류", example = "LOUNGE")
    private PlaceType placeType;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "building_id", example = "null/0/1")
    private Long buildingId;
    @Schema(description = "isBookmarked", example = "true")
    private boolean isBookmarked = false;
    @Schema(description = "categoryColor", example = "red")
    private Color categoryColor;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GlobalSearchRes that = (GlobalSearchRes) o;
        return name.equals(that.name) && locationType.equals(that.locationType);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public GlobalSearchRes(Building building, LocationType locationType, Category category) {
        this.id = building.getId();
        this.name = building.getName();
        this.address = building.getAddress();
        this.longitude = building.getNode().getLongitude();
        this.latitude = building.getNode().getLatitude();
        this.locationType = locationType;
        if(category != null) {
            this.isBookmarked = true;
            this.categoryColor = category.getColor();
        }
    }

    public GlobalSearchRes(Place place, LocationType locationType, boolean hasBuilding, Category category) {
        // 외부태그(편의시설) : id = null, buildingId = 0L, locationType = FACILITY
        if(!hasBuilding && place.getType().getName().equals(place.getName()) && place.getNode() == null) {
            this.id = null;
            this.buildingId = 0L;
            this.name = place.getName();
            // 야외는 노드가 없음
            this.longitude = null;
            this.latitude = null;
            this.locationType = LocationType.FACILITY;
            // 내부태그(편의시설) : id = null, buildingId = 특정 건물 ID, locationType = FACILITY
        } else if (hasBuilding && place.getType().getName().equals(place.getName())) { // 내부태그(편의시설)
            this.id = null;
            this.buildingId = place.getBuilding().getId();
            if(place.getBuilding().getId() == 0) {
                this.name = place.getName();
            } else {
                this.name = place.getBuilding().getName() + " " + place.getName();
            }
            if(place.getBuilding().getNode() != null) {
                this.longitude = place.getBuilding().getNode().getLongitude();
                this.latitude = place.getBuilding().getNode().getLatitude();
            }
            this.locationType = LocationType.FACILITY;
        } else { // 일반 시설들
            this.id = place.getId();
            this.buildingId = place.getBuilding().getId();
            this.floor = place.getFloor();
            if(place.getBuilding().getId() == 0) {
                this.name = place.getName();
            } else {
                this.name = place.getBuilding().getName() + " " + place.getName();
            }
            if(!place.getDetail().equals(".")) this.detail = place.getDetail();
            if(place.getBuilding().getNode() != null) {
                this.longitude = place.getBuilding().getNode().getLongitude();
                this.latitude = place.getBuilding().getNode().getLatitude();
            }
            this.locationType = locationType;
        }
        this.placeType = place.getType();
        if(category != null) {
            this.isBookmarked = true;
            this.categoryColor = category.getColor();
        }
    }
}
