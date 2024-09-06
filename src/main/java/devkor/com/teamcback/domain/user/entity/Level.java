package devkor.com.teamcback.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Level {
    LEVEL1(0), // 0~4점
    LEVEL2(5), // 5~14점
    LEVEL3(15); //15점 이상
    private final int minScore;
}
