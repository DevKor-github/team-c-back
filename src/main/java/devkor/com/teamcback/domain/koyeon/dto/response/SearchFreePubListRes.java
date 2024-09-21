package devkor.com.teamcback.domain.koyeon.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "주점 정보")
@Getter
public class SearchFreePubListRes {
    List<SearchFreePubRes> list;

    public SearchFreePubListRes(List<SearchFreePubRes> list) {
        this.list = list;
    }
}
