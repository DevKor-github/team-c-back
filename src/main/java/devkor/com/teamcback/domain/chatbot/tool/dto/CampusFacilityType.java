package devkor.com.teamcback.domain.chatbot.tool.dto;

import devkor.com.teamcback.domain.place.entity.PlaceType;

public enum CampusFacilityType {
    CLASSROOM,
    TOILET,
    MEN_TOILET,
    WOMEN_TOILET,
    HANDICAPPED_TOILET,
    MEN_HANDICAPPED_TOILET,
    WOMEN_HANDICAPPED_TOILET,
    VENDING_MACHINE,
    WATER_PURIFIER,
    PRINTER,
    LOUNGE,
    CAFE,
    SMOKING_BOOTH,
    CONVENIENCE_STORE,
    CAFETERIA,
    READING_ROOM,
    STUDY_ROOM,
    SLEEPING_ROOM,
    SHOWER_ROOM,
    LOCKER,
    BANK,
    TRASH_CAN,
    GYM,
    BICYCLE_RACK,
    BENCH,
    SHUTTLE_BUS,
    BOOK_RETURN_MACHINE,
    TUMBLER_WASHER,
    ONESTOP_AUTO_MACHINE,
    HEALTH_OFFICE,
    DISABLED_PARKING,
    BARRIER_FREE_ENTRANCE,
    REUSABLE_CUP_RETURN,
    PHARMACY;

    public PlaceType toPlaceType() {
        return PlaceType.valueOf(name());
    }
}
