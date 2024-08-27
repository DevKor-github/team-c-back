package devkor.com.teamcback.domain.place.dto.response;

import devkor.com.teamcback.domain.place.entity.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "강의실 별명 조회 응답 dto")
@Getter
public class GetPlaceNicknameListRes {
    private Long placeId;
    private String placeName;
    private List<GetClassroomNicknameRes> nicknameList;

    public GetPlaceNicknameListRes(Place place, List<GetClassroomNicknameRes> nicknameList) {
        this.placeId = place.getId();
        this.placeName = place.getName();
        this.nicknameList = nicknameList;
    }
}
