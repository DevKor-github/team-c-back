package devkor.com.teamcback.domain.operatingtime.service;

import static devkor.com.teamcback.domain.routes.entity.NodeType.ENTRANCE;
import static devkor.com.teamcback.global.response.ResultCode.OPER_CONDITION_HAS_NO_OPER_TIME;

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
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    public static final String OPERATING_TIME_PATTERN = "^([0-1]?\\d|2[0-3]):[0-5]\\d-([0-1]?\\d|2[0-3]):[0-5]\\d$";

    // 상시 개방 건물
    private static final List<Long> alwaysOpenBuildings = List.of(0L, 23L, 27L, 60L);

    // 출입 권한 필요 혹은 정보가 없는 건물
    private static final List<Long> alwaysAccessRequiredOrWithoutInfoBuildings = List.of(1L, 16L, 17L, 19L, 29L, 31L, 35L, 36L, 37L, 38L, 52L, 54L,
            62L, 63L, 64L, 65L, 66L, 67L, 68L, 69L, 70L, 71L, 72L);
    private static final String DEFAULT_OPERATING_TIME = "00:00-23:59";

    /**
     * 하루에 한 번 (자정) 운영 시간(operatingTime) 설정
     */
    @Transactional
    public void updateOperatingTime(DayOfWeek dayOfWeek, boolean isHoliday, boolean isVacation, boolean isEvenWeek) {
        // 건물 운영 시간 설정
        List<Building> buildings = buildingRepository.findAll();
        for(Building building : buildings) {
            building.setOperatingTime(findBuildingOperatingTime(building, dayOfWeek));
        }

        // 장소 운영 시간 설정
        List<Place> places = placeRepository.findAll();
        for(Place place : places) {
            place.setOperatingTime(findPlaceOperatingTime(place, dayOfWeek));
        }

        // 오늘에 해당하는 운영 조건을 가진 장소들 : 운영 조건 고려
        List<OperatingCondition> operatingConditions = findOperatingConditionList(dayOfWeek, isHoliday, isVacation, isEvenWeek);

        for(OperatingCondition operatingCondition : operatingConditions) {
            operatingCondition.getPlace().setOperatingTime(findPlaceOperatingTimeByCondition(operatingCondition));
        }

    }

    /**
     * 운영 시간에 따른 운영 여부 및 운영 조건을 가지는 장소의 운영 시간 변경
     */
    @Transactional
    public void updateIsOperating(LocalTime now, DayOfWeek dayOfWeek, boolean isHoliday, boolean isVacation, boolean isEvenWeek) {
        List<Building> buildings = buildingRepository.findAll();
        List<Place> placesWithCondition = operatingConditionRepository.findAll().stream().map(OperatingCondition::getPlace).distinct().toList();

        for(Building building : buildings) {
            // 상시 개방이 아닌 건물만 확인
            if(!alwaysOpenBuildings.contains(building.getId())) {
                boolean isOperating = checkBuildingIsOperating(building, now, dayOfWeek);

                // 운영 여부에 변화가 있는 경우
                if(building.isOperating() != isOperating) {
                    log.info("building: {}, isOperating: {}", building.getName(), isOperating);
                    // 출입구 노드 운영 여부 변경
                    changeNodeIsOperating(isOperating, building);
                    // 건물 운영 여부 변경
                    building.setOperating(isOperating);
                    // 건물에 속하면서 장소만의 운영 시간이 없는 경우 운영 여부 동기화
                    List<Place> places = placeRepository.findAllByBuilding(building);
                    for(Place place : places) {
                        if(!placesWithCondition.contains(place)) place.setOperating(isOperating);
                    }
                }
            }
        }

        // 운영 조건을 가진 장소 운영 여부, 운영 시간 변경
        for(Place place : placesWithCondition) {
            OperatingCondition operatingCondition = findOperatingConditionOfPlace(dayOfWeek, isHoliday, isVacation, isEvenWeek, place);
            if(operatingCondition == null) continue; // 운영 조건을 가졌지만 오늘은 영업하지 않는 경우는 수정 x

            setPlaceIsOperatingAndOperatingTime(place, operatingCondition, now);
        }
    }

    /**
     * 건물의 운영 시간 찾기
     */
    private String findBuildingOperatingTime(Building building, DayOfWeek dayOfWeek) {
        // 일자에 해당하는 운영 시간 -> 없으면 다른 요일의 운영 시간을 가져옴
        String operatingTime = dayOfWeek.getOperatingTime(building);
        if(!isTimeRangePattern(operatingTime)) {
            operatingTime = findOtherBuildingOperatingTime(building, dayOfWeek);
        }

        return operatingTime;
    }

    /**
     * 시간 형식인 다음 운영 시간을 찾음
     */
    private String findOtherBuildingOperatingTime(Building building, DayOfWeek dayOfWeek) {
        DayOfWeek nextDay = dayOfWeek.findNext();
        String operatingTime = nextDay.getOperatingTime(building);

        while(dayOfWeek != nextDay) {
            if(isTimeRangePattern(operatingTime)) return operatingTime;
            nextDay = nextDay.findNext();
            operatingTime = nextDay.getOperatingTime(building);
        }

        return DEFAULT_OPERATING_TIME;
    }

    /**
     * 장소의 운영 시간 찾기
     */
    private String findPlaceOperatingTime(Place place, DayOfWeek dayOfWeek) {
        String operatingTime = dayOfWeek.getOperatingTime(place);

        // 장소의 운영 시간이 시간 형식이 아니면 건물의 운영 시간을 따라감
        if(!isTimeRangePattern(operatingTime)) {
            if(place.getBuilding() == null) return DEFAULT_OPERATING_TIME;
            operatingTime = place.getBuilding().getOperatingTime();
        }

        return operatingTime;
    }

    /**
     * 운영 조건에 해당하는 장소의 운영 시간 찾기
     */
    private String findPlaceOperatingTimeByCondition(OperatingCondition operatingCondition) {
        List<OperatingTime> operatingTimeList = operatingTimeRepository.findAllByOperatingCondition(
            operatingCondition);

        LocalTime endTime = LocalTime.of(0, 0,0);
        LocalTime startTime = LocalTime.of(23, 59,59);

        for (OperatingTime operatingTime : operatingTimeList) { // 운영 조건에 해당하는 운영 시간 중 가장 빠른 운영 시간을 적용
            if (startTime.isAfter(operatingTime.getStartTime())) {
                startTime = operatingTime.getStartTime();
                endTime = operatingTime.getEndTime();
            }
        }

        return formatTimeRange(startTime, endTime);
    }

    /**
     * 운영 시간이 00:00-00:00 형식인지 확인
     */
    private boolean isTimeRangePattern(String operatingTime) {
        return operatingTime != null && Pattern.matches(OPERATING_TIME_PATTERN, operatingTime);
    }

    /**
     * 장소의 운영 여부, 운영 시간 찾기
     */
    private void setPlaceIsOperatingAndOperatingTime(Place place, OperatingCondition operatingCondition, LocalTime now) {
        List<OperatingTime> operatingTimeList = operatingTimeRepository.findAllByOperatingCondition(operatingCondition);

        // 운영 여부 판단
        for(OperatingTime operatingTime : operatingTimeList) {
            if(isInOperatingTime(now, operatingTime)) { // 운영 시간에 포함되는 경우
                place.setOperating(true);
                place.setOperatingTime(formatTimeRange(operatingTime.getStartTime(), operatingTime.getEndTime()));
                return;
            }
        }

        // 운영 중이지 않을 경우
        place.setOperating(false);
        OperatingTime op = getNextOperatingTime(operatingTimeList, now);
        place.setOperatingTime(formatTimeRange(op.getStartTime(), op.getEndTime())); // 다음 운영 시간으로 설정
    }

    /**
     * 현재 시간의 next operating time 찾기
     */
    private OperatingTime getNextOperatingTime(List<OperatingTime> operatingTimeList, LocalTime now) {
        // 현재 시간 이후에 시작해서 종료하는 시간을 찾음
        Optional<OperatingTime> nextOperatingTime = operatingTimeList.stream()
            .filter(ot -> ot.getStartTime().isAfter(now))
            .min(Comparator.comparing(OperatingTime::getStartTime));

        // 없으면 가장 처음 시작하는 시간을 선택
        if (nextOperatingTime.isEmpty()) {
            nextOperatingTime = operatingTimeList.stream()
                .min(Comparator.comparing(OperatingTime::getStartTime));
        }

        return nextOperatingTime.orElseThrow(() -> new GlobalException(OPER_CONDITION_HAS_NO_OPER_TIME));
    }

    /**
     * 현재 건물 운영 여부 판단: 운영 시간이 현재 시간을 포함하는지 확인
     */
    private boolean checkBuildingIsOperating(Building building, LocalTime now, DayOfWeek dayOfWeek) {
        // 정보가 없으면 운영 중으로 표시
        if(alwaysAccessRequiredOrWithoutInfoBuildings.contains(building.getId())) {
            return true;
        }

        // 운영 시간이 오늘 일자에 해당하는 건물의 운영시간과 일치하지 않으면 운영 종료 상태
        String operatingTime = building.getOperatingTime();
        if(!operatingTime.equals(dayOfWeek.getOperatingTime(building))) return false;

        return isInOperatingTime(now, operatingTime);
    }

    /**
     * 건물 운영 여부에 따라 출입문 routing 변경
     */
    private void changeNodeIsOperating(boolean isOperating, Building building) {
        log.info("건물 출입문 노드 isOperating 변경");
        List<Node> nodeList = nodeRepository.findAllByBuildingAndType(building, ENTRANCE);
        for(Node node : nodeList) {
            node.setOperating(isOperating);
        }
    }

    /**
     * 오늘, 해당 장소에 맞는 운영 조건 찾기
     */
    private OperatingCondition findOperatingConditionOfPlace(DayOfWeek dayOfWeek, boolean isHoliday, boolean isVacation, boolean isEvenWeek, Place place) {
        OperatingCondition operatingCondition  = operatingConditionRepository.findByDayOfWeekAndIsHolidayAndIsVacationAndPlace(dayOfWeek, isHoliday, isVacation, place);

        if(operatingCondition == null) return null;

        if(dayOfWeek == DayOfWeek.SATURDAY) { // 토요일인 경우
            if(operatingCondition.getIsEvenWeek() == null || operatingCondition.getIsEvenWeek() == isEvenWeek) return operatingCondition;
            else return null;
        }

        return operatingCondition;
    }

    /**
     * 오늘에 맞는 운영 조건 목록 찾기
     */
    private List<OperatingCondition> findOperatingConditionList(DayOfWeek dayOfWeek, boolean isHoliday, boolean isVacation, boolean isEvenWeek) {
        List<OperatingCondition> operatingConditionList  = operatingConditionRepository.findByDayOfWeekAndIsHolidayAndIsVacationOrNot(dayOfWeek, isHoliday, isVacation);

        if(dayOfWeek == DayOfWeek.SATURDAY) { // 토요일인 경우
            if(isEvenWeek) {
                operatingConditionList = operatingConditionList.stream().filter(operatingCondition ->
                    operatingCondition.getIsEvenWeek() == null || operatingCondition.getIsEvenWeek()).toList();
            }
            else {
                operatingConditionList = operatingConditionList.stream().filter(operatingCondition ->
                    operatingCondition.getIsEvenWeek() == null || !operatingCondition.getIsEvenWeek()).toList();
            }
        }

        return operatingConditionList;
    }

    /**
     *
     * 현재가 운영 시간에 포함되는지 판단
     */
    private boolean isInOperatingTime(LocalTime now, OperatingTime operatingTime) {
        return now.isAfter(operatingTime.getStartTime()) && now.isBefore(operatingTime.getEndTime());
    }

    private boolean isInOperatingTime(LocalTime now, String operatingTime) {
        int startHour = Integer.parseInt(operatingTime.substring(0,2));
        int startMinute = Integer.parseInt(operatingTime.substring(3,5));
        int endHour = Integer.parseInt(operatingTime.substring(6,8));
        int endMinute = Integer.parseInt(operatingTime.substring(9,11));

        LocalTime startTime = LocalTime.of(startHour, startMinute);
        LocalTime endTime = LocalTime.of(endHour, endMinute);

        return !now.isBefore(startTime) && now.isBefore(endTime);
    }

    /**
     * 시간 범위를 "HH:mm-HH:mm" 형식으로 지정
     */
    private String formatTimeRange(LocalTime start, LocalTime end) {
        // 포맷을 "HH:mm" 형식으로 지정
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // 각각의 LocalTime을 포맷된 문자열로 변환
        String startFormatted = start.format(formatter);
        String endFormatted = end.format(formatter);

        // 두 시간 문자열을 "HH:mm-HH:mm" 형식으로 연결
        return startFormatted + "-" + endFormatted;
    }

    /**
     * 장소 운영 시간 저장 (건물 운영 시간 변동이 있을 경우에만)
     */
    @Transactional
    public void updatePlaceOperatingTime() {
        List<Place> places = placeRepository.findAll();
        List<Place> placesWithCondition = operatingConditionRepository.findAll().stream().map(OperatingCondition::getPlace).distinct().toList();

        for(Place place : places) {
            if(!placesWithCondition.contains(place)) {
                // 조건이 없는 장소는 건물의 운영 여부 및 운영 시간과 동일하도록 세팅
                place.setSundayOperatingTime(place.getBuilding() == null ? DEFAULT_OPERATING_TIME : place.getBuilding().getSundayOperatingTime());
                place.setSaturdayOperatingTime(place.getBuilding() == null ? DEFAULT_OPERATING_TIME : place.getBuilding().getSaturdayOperatingTime());
                place.setWeekdayOperatingTime(place.getBuilding() == null ? DEFAULT_OPERATING_TIME : place.getBuilding().getWeekdayOperatingTime());
                place.setOperating(place.getBuilding() == null ? true : place.getBuilding().isOperating());
            }
        }
    }
}
