package devkor.com.teamcback.domain.place.dto.response;

import lombok.Getter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
public class GetCafeteriaMenuListRes {
    private Long placeId;
    private String placeName;
    private String address;
    private String operatingTime;
    private String contact;
    private Map<LocalDate, Map<String, String>> menus = new HashMap<>();

    public GetCafeteriaMenuListRes(Long placeId, String name, String address, String operatingTime, String contact, Map<LocalDate, Map<String, String>> menus) {
        this.placeId = placeId;
        this.placeName = name;
        this.address = address;
        this.operatingTime = operatingTime;
        this.contact = contact;
        this.menus = menus;
    }
}
