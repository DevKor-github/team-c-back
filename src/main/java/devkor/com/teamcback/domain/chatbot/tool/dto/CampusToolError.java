package devkor.com.teamcback.domain.chatbot.tool.dto;

public record CampusToolError(CampusToolErrorCode code, String message) {
    public static CampusToolError of(CampusToolErrorCode code) {
        return new CampusToolError(code, switch (code) {
            case NOT_FOUND -> "조건에 맞는 캠퍼스 정보를 찾지 못했습니다.";
            case AMBIGUOUS_LOCATION -> "여러 위치 후보가 있어 사용자의 확인이 필요합니다.";
            case INVALID_INPUT -> "도구 입력값이 올바르지 않습니다.";
            case UNSUPPORTED -> "지원하지 않는 위치 또는 시설 유형입니다.";
            case NO_DATA -> "요청한 조건에 해당하는 데이터가 없습니다.";
            case TEMPORARILY_UNAVAILABLE -> "캠퍼스 정보를 일시적으로 조회할 수 없습니다.";
        });
    }
}
