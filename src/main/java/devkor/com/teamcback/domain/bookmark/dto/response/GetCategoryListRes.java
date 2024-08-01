package devkor.com.teamcback.domain.bookmark.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

@Getter
@Schema(description = "카테고리 목록 조회")
public class GetCategoryListRes {
    List<GetCategoryRes> categoryList;

    public GetCategoryListRes(List<GetCategoryRes> categoryList) {
        this.categoryList = categoryList;
    }
}
