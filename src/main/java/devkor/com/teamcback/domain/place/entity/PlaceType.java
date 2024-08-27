package devkor.com.teamcback.domain.place.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceType {
    CLASSROOM("클래스룸"),
    TOILET("화장실"),
    MEN_TOILET("남자화장실"),
    WOMEN_TOILET("여자화장실"),
    MEN_HANDICAPPED_TOILET("남자장애인화장실"),
    WOMEN_HANDICAPPED_TOILET("여자장애인화장실"),
    VENDING_MACHINE("자판기"),
    WATER_PURIFIER("정수기"),
    PRINTER("프린터"),
    LOUNGE("라운지"),
    CAFE("카페"),
    SMOKING_BOOTH("흡연부스"),
    CONVENIENCE_STORE("편의점"),
    CAFETERIA("식당"),
    READING_ROOM("열람실"),
    STUDY_ROOM("스터디룸"),
    SLEEPING_ROOM("수면실"),
    SHOWER_ROOM("샤워실"),
    LOCKER("사물함"),
    BANK("은행"),
    TRASH_CAN("쓰레기통"),
    GYM("헬스장"),
    BICYCLE_RACK("자전거보관소"),
    BENCH("벤치"),
    SHUTTLE_BUS("셔틀버스정거장"),
    BOOK_RETURN_MACHINE("도서반납기"),
    TUMBLER_WASHER("텀블러세척기"),
    ONESTOP_AUTO_MACHINE("원스탑무인발급기");

    private final String name;
}
