package devkor.com.teamcback.domain.facility.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FacilityType {
    MEN_TOILET("남자화장실"),
    WOMEN_TOILET("여자화장실"),
    MEN_HANDICAPPED_TOILET("남자장애인화장실"),
    WOMEN_HANDICAPPED_TOILET("여자장애인화장실"),
    VENDING_MACHINE("자판기"),
    WATER_PURIFIER("정수기"),
    PRINTER("프린터"),
    LOUNGE("라운지"),
    CAFE("카페"),
    SMOKING_AREA("흡연구역"),
    CONVENIENCE_STORE("편의점"),
    CAFETERIA("식당"),
    READING_ROOM("열람실"),
    STUDY_ROOM("스터디룸"),
    SLEEPING_ROOM("수면실"),
    SHOWER_ROOM("샤워실"),
    LOCKER("사물함"),
    BANK("은행"),
    TRASH_CAN("쓰레기통");

    private final String name;
}
