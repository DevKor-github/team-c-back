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


    MONDAY {
        @Override
        public DayOfWeek findNext() {
            return TUESDAY;
        }

        @Override
        public String getOperatingTime(Building building) {
            return building.getWeekdayOperatingTime();
        }

        @Override
        public String getOperatingTime(Place place) {
            return place.getMondayOperatingTime() == null ? place.getWeekdayOperatingTime() :  place.getMondayOperatingTime();
        }
    },
    TUESDAY {
        @Override
        public DayOfWeek findNext() {
            return WEDNESDAY;
        }

        @Override
        public String getOperatingTime(Building building) {
            return building.getWeekdayOperatingTime();
        }

        @Override
        public String getOperatingTime(Place place) {
            return place.getTuesdayOperatingTime() == null ? place.getWeekdayOperatingTime() :  place.getTuesdayOperatingTime();
        }
    },
    WEDNESDAY {
        @Override
        public DayOfWeek findNext() {
            return THURSDAY;
        }

        @Override
        public String getOperatingTime(Building building) {
            return building.getWeekdayOperatingTime();
        }

        @Override
        public String getOperatingTime(Place place) {
            return place.getWednesdayOperatingTime() == null ? place.getWeekdayOperatingTime() :  place.getWednesdayOperatingTime();
        }
    },
    THURSDAY {
        @Override
        public DayOfWeek findNext() {
            return FRIDAY;
        }

        @Override
        public String getOperatingTime(Building building) {
            return building.getWeekdayOperatingTime();
        }

        @Override
        public String getOperatingTime(Place place) {
            return place.getThursdayOperatingTime() == null ? place.getWeekdayOperatingTime() :  place.getThursdayOperatingTime();
        }
    },
    FRIDAY {
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
            return place.getFridayOperatingTime() == null ? place.getWeekdayOperatingTime() :  place.getFridayOperatingTime();
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
            return MONDAY;
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
