package devkor.com.teamcback.domain.routes.entity;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Conditions {
    BARRIERFREE("배리어프리 여부"),
    SHUTTLE("셔틀 이용 여부"),
    STUDENTCARD("학생증 필요 여부"),
    OPERATING("실시간 운영 여부"),
    INNERROUTE("실내 위주 경로 여부"); //현재는 미구현
    private final String name;
}
