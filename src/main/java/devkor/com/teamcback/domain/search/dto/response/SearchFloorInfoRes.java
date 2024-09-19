package devkor.com.teamcback.domain.search.dto.response;

import java.util.List;
import lombok.Getter;

@Getter
public class SearchFloorInfoRes {
    private List<SearchRoomDetailRes> roomList;
    private List<SearchNodeRes> nodeList;

    public SearchFloorInfoRes(List<SearchRoomDetailRes> roomList) {
        this.roomList = roomList;
        this.nodeList = nodeList;
    }
}
