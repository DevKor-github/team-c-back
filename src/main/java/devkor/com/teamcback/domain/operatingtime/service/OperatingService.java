package devkor.com.teamcback.domain.operatingtime.service;

import static devkor.com.teamcback.domain.routes.entity.NodeType.ENTRANCE;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.routes.entity.Node;
import devkor.com.teamcback.domain.routes.repository.NodeRepository;
import devkor.com.teamcback.domain.operatingtime.entity.DayOfWeek;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingCondition;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingTime;
import devkor.com.teamcback.domain.operatingtime.repositoy.OperatingConditionRepository;
import devkor.com.teamcback.domain.operatingtime.repositoy.OperatingTimeRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j(topic = "Operating Time Service")
@Service
@RequiredArgsConstructor
public class OperatingService {
    private final OperatingConditionRepository operatingConditionRepository;
    private final OperatingTimeRepository operatingTimeRepository;
    private final NodeRepository nodeRepository;
    private final BuildingRepository buildingRepository;
    private final PlaceRepository placeRepository;
    private final EntityManager em;
    private static List<Place> placesWithCondition; // 운영 조건을 가진 장소
    public static final String OPERATING_TIME_PATTERN = "^([0-1]?\\d|2[0-3]):[0-5]\\d-([0-1]?\\d|2[0-3]):[0-5]\\d$";
    // 상시 개방 건물
    private static final List<Long> alwaysOpenBuildings = List.of(0L, 23L, 27L, 60L);

    @Transactional
    public void updateOperatingTime(DayOfWeek dayOfWeek, boolean isHoliday, boolean isVacation, boolean isEvenWeek) {
        // 건물 운영 시간
        List<Building> buildings = buildingRepository.findAll();
        for(Building building : buildings) {
            building.updateOperatingTime(dayOfWeek);
        }

        // 장소 운영 시간
        List<Place> places = placeRepository.findAll();
        for(Place place : places) {
            place.updateOperatingTime(dayOfWeek);
        }

        // 오늘에 해당하는 운영 조건을 가진 장소들
        List<Place> placeList = new ArrayList<>();
        List<OperatingCondition> operatingConditions = findOperatingConditionList(dayOfWeek, isHoliday, isVacation, isEvenWeek);

        for(OperatingCondition operatingCondition : operatingConditions) {
            List<OperatingTime> operatingTimeList = operatingTimeRepository.findAllByOperatingCondition(
                operatingCondition);

            LocalTime endTime = LocalTime.of(0, 0);
            LocalTime startTime = LocalTime.of(23, 59);

            for (OperatingTime operatingTime : operatingTimeList) {
                LocalTime end = LocalTime.of(operatingTime.getEndHour(),
                    operatingTime.getEndMinute());
                LocalTime start = LocalTime.of(operatingTime.getStartHour(),
                    operatingTime.getStartMinute());
                if (endTime.isBefore(end))
                    endTime = end;
                if (startTime.isAfter(start))
                    startTime = start;
            }

            String newOperatingTime = formatTimeRange(startTime, endTime);

            operatingCondition.getPlace().setOperatingTime(newOperatingTime);
            placeList.add(operatingCondition.getPlace());

        }

        placesWithCondition = placeList;
    }

    @Transactional
    public void updateIsOperating(LocalTime now, DayOfWeek dayOfWeek, boolean isHoliday, boolean isVacation, boolean isEvenWeek) {
        List<Building> buildings = buildingRepository.findAll();
        for(Building building : buildings) {
            // 상시 개방이 아닌 건물만 확인
            if(!alwaysOpenBuildings.contains(building.getId())) {
                // 운영 시간이 아닌 다른 문자열 혹은 null인 건물은 false
                if(building.getOperatingTime() == null || !Pattern.matches(OPERATING_TIME_PATTERN, building.getOperatingTime())) {
                    building.setOperating(false);
                }
                // 운영 중인지 확인
                else {
                    boolean isOperating = checkIsOperating(building.getOperatingTime(), now);
                    // 운영 여부에 변화가 있는 경우
                    if(building.isOperating() != isOperating) {
                        log.info("building: {}, isOperating: {}", building.getName(), isOperating);
                        // 출입구 노드 운영 여부 변경
                        changeNodeIsOperating(isOperating, building);
                        // 건물 운영 여부 변경
                        building.setOperating(isOperating);
                        // 건물에 속하면서 장소 만의 운영 시간이 없는 경우 운영 여부 동기화
                        List<Place> places = placeRepository.findAllByBuilding(building);
                        for(Place place : places) {
                            if(!placesWithCondition.contains(place)) place.setOperating(isOperating);
                        }
                    }
                }
            }
        }

        // 운영 조건을 가진 장소 운영 시간 변경
        for(Place place : placesWithCondition) {
            OperatingCondition operatingCondition = findOperatingConditionOfPlace(dayOfWeek, isHoliday, isVacation, isEvenWeek, place);
            place = em.merge(place);
            log.info("is managed: {}", em.contains(place));
            place.setOperating(checkIsOperating(operatingCondition, now));
        }
    }

    // 문자열 운영 시간이 현재 시간을 포함하는지 확인하여 운영 여부 판단
    private boolean checkIsOperating(String operatingTime, LocalTime now) {
        int startHour = Integer.parseInt(operatingTime.substring(0,2));
        int startMinute = Integer.parseInt(operatingTime.substring(3,5));
        int endHour = Integer.parseInt(operatingTime.substring(6,8));
        int endMinute = Integer.parseInt(operatingTime.substring(9,11));

        LocalTime startTime = LocalTime.of(startHour, startMinute);
        LocalTime endTime = LocalTime.of(endHour, endMinute);

        return !now.isBefore(startTime) && now.isBefore(endTime);
    }

    // 건물 운영 여부에 따라 출입문 routing 변경
    private void changeNodeIsOperating(boolean isOperating, Building building) {
        log.info("건물 출입문 노드 isOperating 변경");
        List<Node> nodeList = nodeRepository.findAllByBuildingAndType(building, ENTRANCE);
        for(Node node : nodeList) {
            node.setOperating(isOperating);
        }
    }

    // 오늘에 맞는 운영 조건 찾기
    private OperatingCondition findOperatingConditionOfPlace(DayOfWeek dayOfWeek, boolean isHoliday, boolean isVacation, boolean isEvenWeek, Place place) {
        OperatingCondition operatingCondition  = operatingConditionRepository.findByDayOfWeekAndIsHolidayAndIsVacationAndPlace(dayOfWeek, isHoliday, isVacation, place);

        if(operatingCondition == null) return null;

        if(dayOfWeek == DayOfWeek.SATURDAY) { // 토요일인 경우
            if(operatingCondition.getIsEvenWeek() == null || operatingCondition.getIsEvenWeek() == isEvenWeek) return operatingCondition;
            else return null;
        }

        return operatingCondition;
    }

    private List<OperatingCondition> findOperatingConditionList(DayOfWeek dayOfWeek, boolean isHoliday, boolean isVacation, boolean isEvenWeek) {
        List<OperatingCondition> operatingConditionList  = operatingConditionRepository.findByDayOfWeekAndIsHolidayAndIsVacationOrNot(dayOfWeek, isHoliday, isVacation);

        if(dayOfWeek == DayOfWeek.SATURDAY) { // 토요일인 경우
            if(isEvenWeek) {
                operatingConditionList.stream().filter(operatingCondition ->
                    operatingCondition.getIsEvenWeek() == null || operatingCondition.getIsEvenWeek());
            }
            else {
                operatingConditionList.stream().filter(operatingCondition ->
                    operatingCondition.getIsEvenWeek() == null || !operatingCondition.getIsEvenWeek());
            }
        }

        return operatingConditionList;
    }

    // 운영 시간 확인
    private boolean checkIsOperating(OperatingCondition operatingCondition, LocalTime now) {
        int hour = now.getHour();
        int minute = now.getMinute();
        log.info("hour: {}", hour);
        log.info("minute: {}", minute);

        List<OperatingTime> operatingTimeList = operatingTimeRepository.findAllByOperatingCondition(operatingCondition);

        // 운영 여부 판단
        for(OperatingTime operatingTime : operatingTimeList) {
            if((hour > operatingTime.getStartHour() && hour < operatingTime.getEndHour()) ||
                (hour == operatingTime.getStartHour() && minute >= operatingTime.getStartMinute()) ||
                (hour == operatingTime.getEndHour() && minute < operatingTime.getEndMinute())) {
                return true;
            }
        }
        return false;
    }

    private String formatTimeRange(LocalTime start, LocalTime end) {
        // 포맷을 "HH:mm" 형식으로 지정
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // 각각의 LocalTime을 포맷된 문자열로 변환
        String startFormatted = start.format(formatter);
        String endFormatted = end.format(formatter);

        // 두 시간 문자열을 "HH:mm-HH:mm" 형식으로 연결
        return startFormatted + "-" + endFormatted;
    }

    // 장소 운영 시간 저장(건물 운영 시간 변동이 있을 경우에만)
    @Transactional
    public void updatePlaceOperatingTime() {
        List<Place> places = placeRepository.findAll();
        List<Place> otherPlaces = operatingConditionRepository.findAll().stream().map(OperatingCondition::getPlace).distinct().toList();
        log.info("otherPlaces: {}", otherPlaces.size());

        for(Place place : places) {
            if(!otherPlaces.contains(place)) {
                place.setSundayOperatingTime(place.getBuilding().getSundayOperatingTime());
                place.setSaturdayOperatingTime(place.getBuilding().getSaturdayOperatingTime());
                place.setWeekdayOperatingTime(place.getBuilding().getWeekdayOperatingTime());
            }
        }
    }
}
