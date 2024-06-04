package devkor.com.teamcback.domain.search.dto.response;

import java.util.List;
import lombok.Getter;

@Getter
public class SearchRoomRes {
    private List<GetRoomDetailRes> roomList;

    public SearchRoomRes(List<GetRoomDetailRes> roomList) {
        this.roomList = roomList;
    }
}
