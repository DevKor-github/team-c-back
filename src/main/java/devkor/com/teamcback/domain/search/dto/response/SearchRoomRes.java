package devkor.com.teamcback.domain.search.dto.response;

import java.util.List;
import lombok.Getter;

@Getter
public class SearchRoomRes {
    private List<GetClassroomDetailRes> classroomList;
    private List<GetFacilityDetailRes> facilityList;

    public SearchRoomRes(List<GetClassroomDetailRes> classroomList,
        List<GetFacilityDetailRes> facilityList) {
        this.classroomList = classroomList;
        this.facilityList = facilityList;
    }
}
