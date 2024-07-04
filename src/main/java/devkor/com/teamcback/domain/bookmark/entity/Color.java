package devkor.com.teamcback.domain.bookmark.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import devkor.com.teamcback.global.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Color {
    RED("red", "#ED1C24"),
    ORANGE("orange", "#FF7F27"),
    YELLOW("yellow", "#FFF200"),
    GREEN("green", "#22B14C"),
    BLUE("blue", "#276DDB"),
    PURPLE("purple", "#984EE8"),
    PINK("pink", "#FF64B7"),
    BROWN("brown", "#C9642D"),
    BLACK("black", "#000000");
    private final String name;
    private final String code;

    // Enum Validation 을 위한 코드, enum 에 속하지 않으면 예외처리
    @JsonCreator
    public static Color fromColor(String val) {
        return Arrays.stream(values())
            .filter(color -> color.getName().equals(val))
            .findAny()
            .orElseThrow(() -> new GlobalException(ResultCode.INCORRECT_COLOR));
    }
}
