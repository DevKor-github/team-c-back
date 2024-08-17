package devkor.com.teamcback.domain.classroom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "수정할 교실 정보")
@Getter
@Setter
public class ModifyClassroomReq {
    @Schema(description = "건물 Id", example = "1")
    private Long buildingId;
    @Schema(description = "층 수", example = "1")
    private int floor;
    @Schema(description = "교실 detail", example = "프로그래밍언어연구실")
    private String detail;
    @Schema(description = "교실 이미지", example = "imageUrl")
    private String imageUrl;
    @Schema(description = "교실 이름", example = "101호")
    private String name;
    @Schema(description = "플러그 여부", example = "false")
    private boolean plugAvailability;
    @Schema(description = "노드 Id", example = "1")
    private Long nodeId;
    @Schema(description = "maskIndex", example = "1")
    private Integer maskIndex;
    @Schema(description = "운영여부", example = "true")
    private boolean isOperating;
    @Schema(description = "운영시간", example = "00:00-00:00")
    private String operatingTime;
}
