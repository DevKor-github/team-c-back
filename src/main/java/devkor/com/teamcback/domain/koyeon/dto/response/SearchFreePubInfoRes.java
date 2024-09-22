package devkor.com.teamcback.domain.koyeon.dto.response;

import devkor.com.teamcback.domain.koyeon.entity.FreePub;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Schema(description = "주점 정보")
@Getter
public class SearchFreePubInfoRes {
    @Schema(description = "주점 ID", example = "1")
    private Long id;
    @Schema(description = "주점 이름", example = "맛닭꼬")
    private String name;
    @Schema(description = "후원처", example = "승명호 교우회장님")
    private String sponsor;
    @Schema(description = "주점 경도", example = "127.0274309")
    private Double longitude;
    @Schema(description = "주점 위도", example = "37.5844829")
    private Double latitude;
    @Schema(description = "", example = "서울특별시 성북구 고려대로24길 27")
    private String address;
    @Schema(description = "운영시간", example = "19:00-22:00")
    private String operatingTime;
    @Schema(description = "메뉴List", example = "술, 밥, 치킨")
    private List<String> menus;

    public SearchFreePubInfoRes(FreePub pub) {
        this.id = pub.getId();
        this.name = pub.getName();
        this.sponsor = pub.getSponsor();
        this.latitude = pub.getLatitude();
        this.longitude = pub.getLongitude();
        this.address = pub.getAddress();
        this.operatingTime = pub.getOperatingTime();
        this.menus = Arrays.stream(pub.getMenus().split(",")).toList();
    }
}
