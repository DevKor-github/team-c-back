package devkor.com.teamcback.domain.search.dto.response;

import java.util.List;
import lombok.Getter;

@Getter
public class SearchRoomRes {
    private List<SearchRoomDetailRes> roomList;

    public SearchRoomRes(List<SearchRoomDetailRes> roomList) {
        this.roomList = roomList;
    }
}
