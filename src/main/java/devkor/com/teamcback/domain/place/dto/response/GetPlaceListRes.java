package devkor.com.teamcback.domain.place.dto.response;

import devkor.com.teamcback.domain.place.dto.response.GetPlaceRes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "편의시설 List 조회 응답 dto")
@Getter
public class GetPlaceListRes {
    private List<GetPlaceRes> placeList;

    public GetPlaceListRes(List<GetPlaceRes> placeList) {
        this.placeList = placeList;
    }
}
