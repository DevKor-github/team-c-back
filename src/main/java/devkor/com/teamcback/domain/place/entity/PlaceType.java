package devkor.com.teamcback.domain.place.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceType {
    CLASSROOM("클래스룸", new String[]{}),
    TOILET("화장실", new String[]{}),
    MEN_TOILET("남자화장실", new String[]{}),
    WOMEN_TOILET("여자화장실", new String[]{}),
    MEN_HANDICAPPED_TOILET("남자장애인화장실", new String[]{}),
    WOMEN_HANDICAPPED_TOILET("여자장애인화장실", new String[]{}),
    VENDING_MACHINE("자판기", new String[]{"벤딩머신"}),
    WATER_PURIFIER("정수기", new String[]{"물나오는"}),
    PRINTER("프린터", new String[]{"프린트기", "프린터기", "인쇄기"}),
    LOUNGE("라운지", new String[]{"공부하기좋은곳", "쉴곳", "쉬는"}),
    CAFE("카페", new String[]{"커피"}),
    SMOKING_BOOTH("흡연부스", new String[]{"흡연구역", "담배"}),
    CONVENIENCE_STORE("편의점", new String[]{"컨비니언스스토어"}),
    CAFETERIA("식당", new String[]{"학식", "밥먹는곳"}),
    READING_ROOM("열람실", new String[]{}),
    STUDY_ROOM("스터디룸", new String[]{"단체모임"}),
    SLEEPING_ROOM("수면실", new String[]{"자기좋은곳", "자는곳"}),
    SHOWER_ROOM("샤워실", new String[]{"씻는데"}),
    LOCKER("사물함", new String[]{}),
    BANK("은행", new String[]{}),
    TRASH_CAN("쓰레기통", new String[]{}),
    GYM("헬스장", new String[]{"휘트니스센터", "운동"}),
    BICYCLE_RACK("자전거보관소", new String[]{}),
    BENCH("벤치", new String[]{}),
    SHUTTLE_BUS("셔틀버스정거장", new String[]{"셔틀버스버정"}),
    BOOK_RETURN_MACHINE("도서반납기", new String[]{"책반납기계", "도서반납기계"}),
    TUMBLER_WASHER("텀블러세척기", new String[]{}),
    ONESTOP_AUTO_MACHINE("원스탑무인발급기", new String[]{"원스톱무인발급기", "증명서", "ONE-STOP", "ONESTOP"}),
    KOYEON("고연전주점", new String[]{});

    private final String name;
    private final String[] nickname;
}
