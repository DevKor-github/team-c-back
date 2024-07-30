package devkor.com.teamcback.domain.search.dto.response;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.entity.FacilityType;
import lombok.Getter;

import java.util.List;

@Getter
public class SearchBuildingDetailRes {
    private String name;
    private String address;
    private String details;
    private boolean bookmarked;
    private List<FacilityType> existTypes; //건물 내 facility 종류 리스트(아이콘용)
    private List<GetMainFacilityRes> mainFacilityList;

    public SearchBuildingDetailRes(List<GetMainFacilityRes> facilities, List<FacilityType> types, Building building, boolean bookmarked) {
        this.name = "고려대학교 서울캠퍼스 " + building.getName();
        this.address = building.getAddress();
        this.details = building.getDetail();
        this.bookmarked = bookmarked;
        this.existTypes = types;
        this.mainFacilityList = facilities;
    }
}
