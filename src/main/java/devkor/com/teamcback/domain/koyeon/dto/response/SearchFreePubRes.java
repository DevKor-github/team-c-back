package devkor.com.teamcback.domain.koyeon.dto.response;

import devkor.com.teamcback.domain.koyeon.entity.FreePub;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "주점 정보")
@Getter
public class SearchFreePubRes {
    @Schema(description = "주점 ID", example = "1")
    private Long id;
    @Schema(description = "주점 이름", example = "맛닭꼬")
    private String name;
    @Schema(description = "주점 경도", example = "127.0274309")
    private Double longitude;
    @Schema(description = "주점 위도", example = "37.5844829")
    private Double latitude;

    public SearchFreePubRes(FreePub pub) {
        this.id = pub.getId();
        this.name = pub.getName();
        this.latitude = pub.getLatitude();
        this.longitude = pub.getLongitude();
    }
}
