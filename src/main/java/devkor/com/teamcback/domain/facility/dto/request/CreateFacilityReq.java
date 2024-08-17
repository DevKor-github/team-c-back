package devkor.com.teamcback.domain.facility.dto.request;

import devkor.com.teamcback.domain.facility.entity.FacilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "저장할 편의시설 정보")
@Getter
@Setter
public class CreateFacilityReq {
    @Schema(description = "편의시설 detail", example = "교직원식당")
    private String detail;
    @Schema(description = "층 수", example = "1")
    private int floor;
    @Schema(description = "이미지 URL", example = "imageURL")
    private String imageUrl;
    @Schema(description = "편의시설 이름(보여지는 이름)", example = "자연계 교직원식당")
    private String name;
    @Schema(description = "플러그 사용 가능 여부", example = "true")
    private boolean plugAvailability;
    @Schema(description = "편의시설 type", example = "CAFETERIA")
    private FacilityType type;
    @Schema(description = "건물 Id", example = "1")
    private Long buildingId;
    @Schema(description = "노드 Id", example = "1")
    private Long nodeId;
    @Schema(description = "maskIndex", example = "1")
    private Integer maskIndex;
    @Schema(description = "운영시간", example = "11:00-14:00")
    private String operatingTime;
    @Schema(description = "현재 운영 여부", example = "true")
    private boolean isOperating;
    @Schema(description = "편의시설 사용 가능 여부", example = "true")
    private boolean availability;
}
