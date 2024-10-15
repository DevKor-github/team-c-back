package devkor.com.teamcback.domain.routes.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NodeType {
    NORMAL("일반"),
    ELEVATOR("엘리베이터"),
    STAIR("계단"),
    ENTRANCE("출입문"),
    CHECKPOINT("경로"),
    SHUTTLE("셔틀"),
    EVENT("이벤트 노드");

    private final String name;
}
