package devkor.com.teamcback.domain.suggestion.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuggestionType {
    LOCATION_ERROR("시설 위치 오류"),
    INCORRECT_NAME("잘못된 시설명"),
    INCORRECT_OPERATION_HOURS("잘못된 운영시간"),
    CLOSED_PLACE("없거나 사라진 장소"),
    INCORRECT_ROUTE("잘못된 경로 안내"),
    ADDITIONAL_INFORMATION("추가하고 싶은 정보"),
    FUNCTIONAL_ERROR("기능 오류"),
    FEATURE_SUGGESTION("기능 추가 요청"),
    INCONVENIENCE("불편한 점"),
    QUESTION("궁금한 점"),
    OTHER("기타");
    private final String type;
}
