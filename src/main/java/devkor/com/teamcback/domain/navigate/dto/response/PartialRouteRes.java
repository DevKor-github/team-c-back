package devkor.com.teamcback.domain.navigate.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PartialRouteRes {
    @Schema(description = "실내/실외 판별 bool. true이면 실내.", example = "true")
    public boolean inOut;
    //내부의 경우 빌딩 id, 층 제공
    @Schema(description = "건물 id, 실내일 경우에만 제공", example = "1")
    public Long buildingId;
    @Schema(description = "건물 층, 실내일 경우에만 제공", example = "3")
    public Double floor;
    //경로 리스트
    @Schema(description = "상세 경로. 실내의 경우 x좌표-y좌표-id, 실외의 경우 위도-경도-id로 제공", example =
        """
                [
					[
						251.0,
						183.0,
						99.0
					],
					[
						282.0,
						221.0,
						81.0
					],
					[
						260.0,
						256.0,
						79.0
					],
					[
						227.0,
						280.0,
						77.0
					],
					[
						187.0,
						289.0,
						74.0
					],
					[
						147.0,
						282.0,
						71.0
					],
					[
						149.0,
						316.0,
						73.0
					]
				]
            """)
    public List<List<Double>> route;
    //설명
    @Setter
    @Schema(description = "경로 대략적 설명", example = "도착")
    public String info;

    public PartialRouteRes(Long buildingId, Double floor, List<List<Double>> route){
        this.inOut = true;
        this.buildingId = buildingId;
        this.floor = floor;
        this.route = route;
    }

    public PartialRouteRes(List<List<Double>> route){
        this.inOut = false;
        this.route = route;
    }

}
