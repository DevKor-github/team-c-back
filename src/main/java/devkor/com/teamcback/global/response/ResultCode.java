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
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, 1007, "지원하는 파일 형식이 아닙니다."),
    MAXIMUM_UPLOAD_FILE_SIZE(HttpStatus.BAD_REQUEST, 1008, "파일 크기는 최대 10MB까지 가능합니다."),
    NOT_FOUND_FILE(HttpStatus.NOT_FOUND, 1009, "파일을 찾을 수 없습니다."),

    // 사용자 2000번대
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, 2000, "사용자를 찾을 수 없습니다."),

    // 건물 3000번대
    NOT_FOUND_BUILDING(HttpStatus.NOT_FOUND, 3000, "건물을 찾을 수 없습니다."),
    NOT_FOUND_ENTRANCE(HttpStatus.NOT_FOUND, 3001, "건물 출입구를 찾을 수 없습니다."),
    DUPLICATED_BUILDING_IMAGE(HttpStatus.CONFLICT, 3002, "해당 건물의 층의 이미지가 이미 존재합니다."),
    INCORRECT_FLOOR(HttpStatus.BAD_REQUEST, 3003, "건물에 존재하지 않는 층입니다."),
    NOT_FOUND_BUILDING_IMAGE(HttpStatus.NOT_FOUND, 3004, "존재하지 않는 건물 사진입니다."),
    NOT_FOUND_BUILDING_NICKNAME(HttpStatus.NOT_FOUND, 3005, "건물 별명을 찾을 수 없습니다."),

    // 강의실 4000번대
    NOT_FOUND_CLASSROOM(HttpStatus.NOT_FOUND, 4000, "강의실을 찾을 수 없습니다."),
    NOT_FOUND_CLASSROOM_NICKNAME(HttpStatus.NOT_FOUND, 4001, "강의실 별명을 찾을 수 없습니다."),

    // 편의시설 5000번대
    NOT_FOUND_FACILITY(HttpStatus.NOT_FOUND, 5000, "편의시설을 찾을 수 없습니다."),

    // 길찾기 6000번대
    NOT_FOUND_NODE(HttpStatus.NOT_FOUND, 6000, "노드를 찾을 수 없습니다."),
    NOT_FOUND_ROUTE(HttpStatus.BAD_REQUEST, 6001, "경로를 찾을 수 없습니다."),
    NOT_PROVIDED_ROUTE(HttpStatus.BAD_REQUEST, 6002, "해당 경로는 제공되지 않습니다."),

    // 즐겨찾기 7000번대
    INCORRECT_COLOR(HttpStatus.BAD_REQUEST, 7000, "정해진 색상 내에서 선택해주세요."),
    NOT_FOUND_CATEGORY(HttpStatus.NOT_FOUND, 7001, "카테고리를 찾을 수 없습니다."),
    NOT_FOUND_BOOKMARK(HttpStatus.NOT_FOUND, 7002, "즐겨찾기를 찾을 수 없습니다."),
    DUPLICATED_BOOKMARK(HttpStatus.CONFLICT, 7003, "해당 즐겨찾기가 이미 존재합니다."),
    NOT_FOUND_IN_CATEGORY(HttpStatus.NOT_FOUND, 7004, "카테고리 내에 해당 즐겨찾기가 존재하지 않습니다.")
    ;

    private final HttpStatus status;
    private final int code;
    private final String message;
}
