package devkor.com.teamcback.domain.navigate.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NodeType {
    NORMAL("일반"),
    ELEVATOR("엘리베이터"),
    STAIR("계단"),
    ENTRANCE("출입문");

    private final String name;
}
