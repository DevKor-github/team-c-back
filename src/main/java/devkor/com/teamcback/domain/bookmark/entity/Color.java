package devkor.com.teamcback.domain.bookmark.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Color {
    RED("red"),
    ORANGE("orange"),
    YELLOW("yellow"),
    GREEN("green"),
    BLUE("blue"),
    PURPLE("purple"),
    PINK("pink"),
    BROWN("brown"),
    BLACK("black");
    private final String name;

    // Enum Validation 을 위한 코드, enum 에 속하지 않으면 예외처리
    @JsonCreator
    public static Color fromColor(String val) {
        return Arrays.stream(values())
            .filter(color -> color.getName().equals(val))
            .findAny()
            .orElseThrow(() -> new GlobalException(ResultCode.INCORRECT_COLOR));
    }
}
