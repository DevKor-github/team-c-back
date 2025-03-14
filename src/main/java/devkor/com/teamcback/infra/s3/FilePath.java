package devkor.com.teamcback.infra.s3;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FilePath { // 파일 경로를 나타내는 상수를 정의
    PROFILE("profile/"),
    BUILDING("building/"),
    PLACE("place/"),
    BUILDING_IMAGE("buildingImage/"),
    SUGGESTION("suggestion/");

    private final String path; // 경로를 저장하는 final 필드
}
