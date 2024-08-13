package devkor.com.teamcback.domain.admin.facility.dto.response;

import devkor.com.teamcback.domain.facility.entity.Facility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "편의시설 생성 응답 dto")
@Getter
public class CreateFacilityRes {
    private Long facilityID;

    public CreateFacilityRes(Facility facility) {
        this.facilityID = facility.getId();
    }
}
