package devkor.com.teamcback.domain.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

@Getter
@Schema(description = "검색 기록 목록")
public class SearchLogListRes {
    List<SearchLogRes> list;

    public SearchLogListRes(List<SearchLogRes> list) {
        this.list = list;
    }
}
