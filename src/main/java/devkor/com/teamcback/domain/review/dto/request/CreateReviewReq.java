package devkor.com.teamcback.domain.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "저장할 리뷰 정보")
@Getter
@Setter
public class CreateReviewReq {

    @Schema(description = "별점", example = "1.5")
    @DecimalMin(value = "0.5", message = "별점을 입력해주세요.")
    private double score;

    @Schema(description = "재방문 여부", example = "false")
    private boolean isRevisit = false;

    @Schema(description = "리뷰 태그 리스트")
    @Size(max = 5, message = "태그는 최대 5개까지 가능합니다.")
    private List<Long> tagIds = new ArrayList<>();

    @Schema(description = "한줄평", example = "맛있고 좋아요.")
    private String comment;

    @Size(max = 3)
    @Schema(description = "첨부 사진")
    private List<MultipartFile> images = new ArrayList<>();
}
