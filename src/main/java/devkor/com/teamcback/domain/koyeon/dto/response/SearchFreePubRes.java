package devkor.com.teamcback.domain.koyeon.dto.response;

import devkor.com.teamcback.domain.koyeon.entity.FreePub;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "주점 정보")
@Getter
public class SearchFreePubRes {
    @Schema(description = "주점 ID", example = "1")
    private Long id;
    @Schema(description = "주점 이름", example = "맛닭꼬")
    private String name;
    @Schema(description = "후원 단체", example = "79학번 동기회")
    private String sponsor;
    @Schema(description = "운영 시간", example = "19:00-22:00")
    private String operatingTime;
    @Schema(description = "주점 경도", example = "127.0274309")
    private Double longitude;
    @Schema(description = "주점 위도", example = "37.5844829")
    private Double latitude;
    @Schema(description = "노드 ID", example = "1")
    private Long nodeId = null;
    @Schema(description = "태그에 해당하는 음식 리스트", example = "[\"떡볶이\"]")
    private List<String> filteredMenus = new ArrayList<>();

    public SearchFreePubRes(FreePub pub) {
        this.id = pub.getId();
        this.name = pub.getName();
        this.sponsor = pub.getSponsor();
        this.operatingTime = pub.getOperatingTime();
        this.latitude = pub.getLatitude();
        this.longitude = pub.getLongitude();
        if(pub.getNode() != null) this.nodeId = pub.getNode().getId();
    }

    public SearchFreePubRes(FreePub pub, List<String> filteredMenus) {
        this.id = pub.getId();
        this.name = pub.getName();
        this.sponsor = pub.getSponsor();
        this.operatingTime = pub.getOperatingTime();
        this.latitude = pub.getLatitude();
        this.longitude = pub.getLongitude();
        if(pub.getNode() != null) this.nodeId = pub.getNode().getId();
        this.filteredMenus = filteredMenus;
    }
}
