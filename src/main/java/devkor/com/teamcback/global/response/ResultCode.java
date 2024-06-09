package devkor.com.teamcback.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResultCode {
    // 글로벌 1000번대
    SUCCESS(HttpStatus.OK, 0, "정상 처리 되었습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, 1000, "잘못된 입력값입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 1001, "권한이 없는 사용자입니다."),
    REFRESH_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, 1002, "Refresh Token이 필요합니다."),
    LOG_IN_REQUIRED(HttpStatus.UNAUTHORIZED, 1004, "다시 로그인 해주세요."),
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1005, "서버 시스템 문제가 발생했습니다."),
    ILLEGAL_REGISTRATION_ID(HttpStatus.BAD_REQUEST, 1006, "지원하는 소셜이 아닙니다."),

    // 사용자 2000번대
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, 2000, "사용자를 찾을 수 없습니다."),

    // 건물 3000번대
    NOT_FOUND_BUILDING(HttpStatus.NOT_FOUND, 3000, "건물을 찾을 수 없습니다."),
    NOT_FOUND_ENTRANCE(HttpStatus.NOT_FOUND, 3001, "건물 출입구를 찾을 수 없습니다."),

    // 강의실 4000번대
    NOT_FOUND_CLASSROOM(HttpStatus.NOT_FOUND, 4000, "강의실을 찾을 수 없습니다."),

    // 편의시설 5000번대
    NOT_FOUND_FACILITY(HttpStatus.NOT_FOUND, 5000, "편의시설을 찾을 수 없습니다."),

    // 길찾기 6000번대
    NOT_FOUND_NODE(HttpStatus.NOT_FOUND, 6000, "노드를 찾을 수 없습니다."),
    NOT_FOUND_ROUTE(HttpStatus.BAD_REQUEST, 6001, "경로를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
