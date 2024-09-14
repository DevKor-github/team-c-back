package devkor.com.teamcback.domain.operatingtime.entity;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.place.entity.Place;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DayOfWeek {
    WEEKDAY {
        @Override
        public DayOfWeek findNext() {
            return SATURDAY;
        }

        @Override
        public String getOperatingTime(Building building) {
            return building.getWeekdayOperatingTime();
        }

        @Override
        public String getOperatingTime(Place place) {
            return place.getWeekdayOperatingTime();
        }
    },
    SATURDAY {
        @Override
        public DayOfWeek findNext() {
            return SUNDAY;
        }

        @Override
        public String getOperatingTime(Building building) {
            return building.getSaturdayOperatingTime();
        }

        @Override
        public String getOperatingTime(Place place) {
            return place.getSaturdayOperatingTime();
        }
    },
    SUNDAY {
        @Override
        public DayOfWeek findNext() {
            return WEEKDAY;
        }

        @Override
        public String getOperatingTime(Building building) {
            return building.getSundayOperatingTime();
        }

        @Override
        public String getOperatingTime(Place place) {
            return place.getSundayOperatingTime();
        }
    };

    public abstract DayOfWeek findNext();
    public abstract String getOperatingTime(Building building);
    public abstract String getOperatingTime(Place place);
}
