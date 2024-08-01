package devkor.com.teamcback.domain.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Schema(description = "키워드 검색 결과")
@Getter
public class GlobalSearchListRes {
    List<GlobalSearchRes> list;

    public GlobalSearchListRes(List<GlobalSearchRes> list) {
        this.list = list;
    }
}
