package devkor.com.teamcback.domain.koyeon.dto.response;

import devkor.com.teamcback.domain.koyeon.entity.FreePub;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.Objects;

@Schema(description = "검색 결과")
@Getter
public class GlobalPubSearchRes {
    @Schema(description = "주점 ID", example = "1")
    private Long id;
    @Schema(description = "주점 이름", example = "맛닭꼬")
    private String name;
    @Schema(description = "후원처", example = "승명호 교우회장님")
    private String sponsor;
    @Schema(description = "", example = "서울특별시 성북구 고려대로24길 27")
    private String address;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GlobalPubSearchRes that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }

    public GlobalPubSearchRes(FreePub pub) {
        this.id = pub.getId();
        this.name = pub.getName();
        this.sponsor = pub.getSponsor();
        this.address = pub.getAddress();
    }

}
