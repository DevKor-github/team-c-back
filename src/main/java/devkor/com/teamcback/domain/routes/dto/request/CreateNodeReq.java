package devkor.com.teamcback.domain.routes.dto.request;

import devkor.com.teamcback.domain.routes.entity.NodeType;
import devkor.com.teamcback.domain.routes.validation.CommaNumeric;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "저장할 노드 정보")
@Getter
@Setter
public class CreateNodeReq {
    @Schema(description = "노드 타입", example = "NORMAL")
    private NodeType type;
    @Schema(description = "x좌표", example = "0.0")
    private Double xCoord;
    @Schema(description = "y좌표", example = "0.0")
    private Double yCoord;
    @Schema(description = "위도", example = "0.0")
    private Double latitude;
    @Schema(description = "경도", example = "0.0")
    private Double longitude;
    @Schema(description = "층수", example = "1")
    private Double floor;
    @Schema(description = "길찾기 노드", example = "true")
    private boolean routing;
    @Schema(description = "건물ID", example = "1")
    private Long buildingId;
    @Schema(description = "인접 노드", example = "1,2,3")
    @CommaNumeric
    private String adjacentNode;
    @Schema(description = "인접 노드와의 거리", example = "10,12,5")
    @CommaNumeric
    private String distance;
}
