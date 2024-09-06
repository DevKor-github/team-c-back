package devkor.com.teamcback.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Level {
    LEVEL1(0), // 0~4점
    LEVEL2(5), // 5~14점
    LEVEL3(15), //15~24점
    LEVEL4(25), //25~34점
    LEVEL5(35); //35점 이상
    private final int minScore;
}
