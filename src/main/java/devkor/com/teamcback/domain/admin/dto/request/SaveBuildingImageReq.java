package devkor.com.teamcback.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "저장할 건물 내부 사진 정보")
@Getter
public class SaveBuildingImageReq {
    private Long buildingId;
    private Double floor;
}
