package devkor.com.teamcback.domain.search.dto.response;

import java.util.List;
import lombok.Getter;

@Getter
public class SearchFloorInfoRes {
    private List<SearchRoomDetailRes> roomList;

    public SearchFloorInfoRes(List<SearchRoomDetailRes> roomList) {
        this.roomList = roomList;
    }
}
