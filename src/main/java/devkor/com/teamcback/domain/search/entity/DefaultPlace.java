package devkor.com.teamcback.domain.search.entity;

import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.global.exception.GlobalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static devkor.com.teamcback.global.response.ResultCode.INCORRECT_LEVEL;
import static devkor.com.teamcback.global.response.ResultCode.NOT_SUPPORTED_PLACE_TYPE;

@Component
public class DefaultPlace {
    @Value("${place.default-image.cafe}")
    private String cafeUrl;

    @Value("${place.default-image.cafeteria}")
    private String cafeteriaUrl;

    @Value("${place.default-image.convenience-store}")
    private String convenienceStoreUrl;

    @Value("${place.default-image.gym}")
    private String gymUrl;

    @Value("${place.default-image.lounge}")
    private String loungeUrl;

    @Value("${place.default-image.reading-room}")
    private String readingRoomUrl;

    @Value("${place.default-image.shower-room}")
    private String showerRoomUrl;

    @Value("${place.default-image.sleeping-room}")
    private String sleepingRoomUrl;

    @Value("${place.default-image.study-room}")
    private String studyRoomUrl;


    private static DefaultPlace defaultPlace;

    public DefaultPlace() {
        defaultPlace = this;
    }

    public static String getUrlByPlaceType(PlaceType type) {
        return switch (type) {
            case CAFE -> defaultPlace.cafeUrl;
            case CAFETERIA -> defaultPlace.cafeteriaUrl;
            case CONVENIENCE_STORE -> defaultPlace.convenienceStoreUrl;
            case GYM -> defaultPlace.gymUrl;
            case LOUNGE -> defaultPlace.loungeUrl;
            case READING_ROOM -> defaultPlace.readingRoomUrl;
            case SHOWER_ROOM -> defaultPlace.showerRoomUrl;
            case SLEEPING_ROOM -> defaultPlace.sleepingRoomUrl;
            case STUDY_ROOM -> defaultPlace.studyRoomUrl;
            default -> throw new GlobalException(NOT_SUPPORTED_PLACE_TYPE);
        };
    }
}

