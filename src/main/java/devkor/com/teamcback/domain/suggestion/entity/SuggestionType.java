package devkor.com.teamcback.domain.suggestion.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuggestionType {
    LOCATION_ERROR("시설 위치 오류"),
    INCORRECT_NAME("잘못된 시설 명"),
    INCORRECT_OPERATION_HOURS("잘못된 운영 시간"),
    CLOSED_PLACE("없거나 사라진 장소"),
    INCORRECT_ROUTE("잘못된 경로 안내"),
    ADDITIONAL_INFORMATION("추가하고 싶은 정보"),
    FUNCTIONAL_ERROR("기능 오류"),
    FEATURE_SUGGESTION("추천 기능"),
    INCONVENIENCE("불편 사항"),
    QUESTION("질의 사항"),
    OTHER("기타");
    private final String type;
}
