package devkor.com.teamcback.domain.operatingtime.service;

import static devkor.com.teamcback.domain.navigate.entity.NodeType.ENTRANCE;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.classroom.repository.ClassroomRepository;
import devkor.com.teamcback.domain.facility.entity.Facility;
import devkor.com.teamcback.domain.facility.repository.FacilityRepository;
import devkor.com.teamcback.domain.navigate.entity.Node;
import devkor.com.teamcback.domain.navigate.repository.NodeRepository;
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
    private final ClassroomRepository classroomRepository;
    private final FacilityRepository facilityRepository;
    private final EntityManager entityManager;

    private static List<OperatingCondition> operatingConditionList = new ArrayList<>();
    private static List<Classroom> notOperatingClassrooms = new ArrayList<>();
    private static List<Facility> notOperatingFacilities = new ArrayList<>();

    @Transactional
    public void updateOperatingTime(DayOfWeek dayOfWeek, boolean isHoliday, boolean isVacation, boolean evenWeek) {
        List<Building> buildingList = new ArrayList<>();
        List<Classroom> classroomList = new ArrayList<>();
        List<Facility> facilityList = new ArrayList<>();

        operatingConditionList  = findOperatingCondition(dayOfWeek, isHoliday, isVacation, evenWeek);

        for(OperatingCondition operatingCondition : operatingConditionList) {
            List<OperatingTime> operatingTimeList = operatingTimeRepository.findAllByOperatingCondition(operatingCondition);

            LocalTime endTime = LocalTime.of(0, 0);
            LocalTime startTime = LocalTime.of(23, 59);

            for(OperatingTime operatingTime : operatingTimeList) {
                LocalTime end = LocalTime.of(operatingTime.getEndHour(), operatingTime.getEndMinute());
                LocalTime start = LocalTime.of(operatingTime.getStartHour(), operatingTime.getStartMinute());
                if(endTime.isBefore(end)) endTime = end;
                if(startTime.isAfter(start)) startTime = start;
            }

            String newOperatingTime = formatTimeRange(startTime, endTime);

            if(operatingCondition.getBuilding() != null) {
                operatingCondition.getBuilding().setOperatingTime(newOperatingTime);
                buildingList.add(operatingCondition.getBuilding());
            }
            else if(operatingCondition.getClassroom() != null) {
                operatingCondition.getClassroom().setOperatingTime(newOperatingTime);
                classroomList.add(operatingCondition.getClassroom());
            }
            else if(operatingCondition.getFacility() != null) {
                operatingCondition.getFacility().setOperatingTime(newOperatingTime);
                facilityList.add(operatingCondition.getFacility());
            }
        }

        // 운영조건에 포함되지 못한 건물, 강의실, 편의시설
        List<Building> notOperatingBuildings = buildingRepository.findAllByIdNotIn(buildingList.stream().map(Building::getId).toList());
        for(Building building : notOperatingBuildings) {
            if(building.getId() != 0) building.setOperating(false);
        }

        if(classroomList.isEmpty()) notOperatingClassrooms = classroomRepository.findAll();
        else notOperatingClassrooms = classroomRepository.findAllByIdNotIn(classroomList.stream().map(Classroom::getId).toList());

        if(facilityList.isEmpty()) notOperatingFacilities = facilityRepository.findAll();
        else notOperatingFacilities = facilityRepository.findAllByIdNotIn(facilityList.stream().map(Facility::getId).toList());

    }

    @Transactional
    public void updateIsOperating(LocalDateTime now) {
        for(OperatingCondition operCondition : operatingConditionList) {
            boolean isOperating = checkOperatingTime(operCondition, now);
            log.info("isOperating: {}", isOperating);

            if(operCondition.getBuilding() != null) {
                Building building = operCondition.getBuilding();
                building = entityManager.merge(building);
                log.info("building: {}", building.getName());
                if(building.isOperating() != isOperating) {
                    building.setOperating(isOperating);
                    changeNodeIsOperating(isOperating, building);
                }
            }
            else if(operCondition.getClassroom() != null) {
                Classroom classroom = operCondition.getClassroom();
                classroom = entityManager.merge(classroom);
                log.info("classroom: {}", classroom.getName());
                if(classroom.isOperating() != isOperating) {
                    classroom.setOperating(isOperating);
                }
            }
            else if(operCondition.getFacility() != null) {
                Facility facility = operCondition.getFacility();
                facility = entityManager.merge(facility);
                log.info("facility: {}", facility.getName());
                if(facility.isOperating() != isOperating) {
                    facility.setOperating(isOperating);
                }
            }
        }

        // 운영 조건에 포함되지 않은 강의실, 편의시설
        for(Classroom classroom : notOperatingClassrooms) {
            classroom = entityManager.merge(classroom);
            classroom.setOperating(classroom.getBuilding().isOperating()); // 건물 운영여부를 따라감
        }

        for(Facility facility : notOperatingFacilities) {
            facility = entityManager.merge(facility);
            facility.setOperating(facility.getBuilding().isOperating()); // 건물 운영여부를 따라감
        }
    }

    private List<OperatingCondition> findOperatingCondition(DayOfWeek dayOfWeek, boolean isHoliday, boolean isVacation, boolean evenWeek) {
        List<OperatingCondition> operatingConditionList  = operatingConditionRepository.findAllByDayOfWeekAndIsHolidayAndIsVacationOrNot(dayOfWeek, isHoliday, isVacation);

        if(dayOfWeek == DayOfWeek.SATURDAY) { // 토요일인 경우
            if(evenWeek) {
                operatingConditionList.stream().filter(operatingCondition ->
                    operatingCondition.getIsEvenWeek() == null || operatingCondition.getIsEvenWeek() == true);
            }
            else {
                operatingConditionList.stream().filter(operatingCondition ->
                    operatingCondition.getIsEvenWeek() == null || operatingCondition.getIsEvenWeek() == false);
            }
        }

        return operatingConditionList;
    }

    // 운영 시간 확인
    private boolean checkOperatingTime(OperatingCondition operatingCondition, LocalDateTime now) {
        int hour = now.getHour();
        int minute = now.getMinute();
        log.info("hour: {}", hour);
        log.info("minute: {}", minute);

        List<OperatingTime> operatingTimeList = operatingTimeRepository.findAllByOperatingCondition(operatingCondition);

        // 운영 여부 판단
        for(OperatingTime operatingTime : operatingTimeList) {
            if((hour > operatingTime.getStartHour() && hour < operatingTime.getEndHour()) ||
                (hour == operatingTime.getStartHour() && minute >= operatingTime.getStartMinute()) ||
                (hour == operatingTime.getEndHour() && minute <= operatingTime.getEndMinute())) {
                return true;
            }
        }
        return false;
    }

    // 건물 운영 여부에 따라 출입문 routing 변경
    private void changeNodeIsOperating(boolean isOperating, Building building) {
        log.info("건물 출입문 노드 isOperating 변경");
        List<Node> nodeList = nodeRepository.findAllByBuildingAndType(building, ENTRANCE);
        for(Node node : nodeList) {
            node.setOperating(isOperating);
        }
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
}
