package devkor.com.teamcback.domain.search.dto.response;

import java.util.List;
import lombok.Getter;

@Getter
public class SearchRoomRes {
    private List<SearchRoomDetailRes> roomList;
    private List<SearchNodeRes> nodeList;

    public SearchRoomRes(List<SearchRoomDetailRes> roomList, List<SearchNodeRes> nodeList) {
        this.roomList = roomList;
        this.nodeList = nodeList;
    }
}
