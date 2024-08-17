package devkor.com.teamcback.domain.facility.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "편의시설 List 조회 응답 dto")
@Getter
public class GetFacilityListRes {
    private List<GetFacilityRes> facilityList;

    public GetFacilityListRes(List<GetFacilityRes> facilityList) {
        this.facilityList = facilityList;
    }
}
