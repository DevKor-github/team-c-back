package devkor.com.teamcback.domain.bookmark.dto.response;

import devkor.com.teamcback.domain.bookmark.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "카테고리 생성 완료")
@Getter
public class CreateCategoryRes {
    private Long categoryId;

    public CreateCategoryRes(Category category) {
        this.categoryId = category.getId();
    }
}
