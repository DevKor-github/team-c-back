package devkor.com.teamcback.domain.search.service;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.entity.BuildingNickname;
import devkor.com.teamcback.domain.building.repository.BuildingNicknameRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.search.dto.response.AutoCompleteRes;
import devkor.com.teamcback.domain.search.entity.PlaceType;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final BuildingRepository buildingRepository;
    private final BuildingNicknameRepository buildingNicknameRepository;

    @Transactional(readOnly = true)
    public List<AutoCompleteRes> autoComplete(String word) {
        // 건물 조회
        List<BuildingNickname> buildingNicknames = buildingNicknameRepository.findByNicknameContaining(word);

        List<AutoCompleteRes> resList = new ArrayList<>();

        // 중복을 제거하고 Building 객체만 추출하여 List에 저장
        List<Building> uniqueBuildings = buildingNicknames.stream()
            .map(BuildingNickname::getBuilding)
            .distinct()
            .toList();

        // TotalSearchRes 객체를 생성하여 결과 리스트에 추가
        return uniqueBuildings.stream()
            .map(building -> new AutoCompleteRes(building, PlaceType.BUILDING))
            .toList();
    }
}
