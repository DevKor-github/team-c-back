package devkor.com.teamcback.domain.place.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FoodType {

    KOREAN("한식"),
    WESTERN("양식"),
    JAPANESE("일식"),
    CHINESE("중식"),
    ASIAN("아시안"),
    FUSION("퓨전"),
    CAFE("카페"),
    BAKERY("베이커리"),
    SALAD("샐러드"),
    PORRIDGE("죽"),
    BBQ("고기요리"),
    BUNSIK("분식"),
    CHICKEN("치킨"),
    PIZZA("피자"),
    BURGER("햄버거"),
    SEAFOOD("해산물"),
    NOODLES("면요리"),
    BISTRO("요리주점"),
    BAR("바");

    private final String type;

}
