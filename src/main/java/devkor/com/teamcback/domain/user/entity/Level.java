package devkor.com.teamcback.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Level {
    LEVEL1(0, 1, ProfileImage.getUrlByLevel(1)), // 0~4점
    LEVEL2(5, 2, ProfileImage.getUrlByLevel(2)), // 5~14점
    LEVEL3(15, 3, ProfileImage.getUrlByLevel(3)), //15~24점
    LEVEL4(25, 4, ProfileImage.getUrlByLevel(4)), //25~34점
    LEVEL5(35, 5, ProfileImage.getUrlByLevel(5)), ; //35점 이상
    private final int minScore;
    private final int levelNumber;
    private final String profileImage;
}