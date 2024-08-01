package devkor.com.teamcback.domain.operatingtime.service;

import static devkor.com.teamcback.domain.navigate.entity.NodeType.ENTRANCE;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.navigate.entity.Node;
import devkor.com.teamcback.domain.navigate.repository.NodeRepository;
import devkor.com.teamcback.domain.operatingtime.entity.DayOfWeek;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingCondition;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingTime;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingWeekend;
import devkor.com.teamcback.domain.operatingtime.repositoy.OperatingConditionRepository;
import devkor.com.teamcback.domain.operatingtime.repositoy.OperatingTimeRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    @Transactional
    public void updateOperatingTime(DayOfWeek dayOfWeek, Boolean isVacation, Boolean evenWeek) {
        List<OperatingCondition> operatingConditionList  = findOperatingCondition(dayOfWeek, isVacation, evenWeek);

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
            }
            else if(operatingCondition.getClassroom() != null) {
                operatingCondition.getClassroom().setOperatingTime(newOperatingTime);
            }
            else if(operatingCondition.getFacility() != null) {
                operatingCondition.getFacility().setOperatingTime(newOperatingTime);
            }
        }
    }

    @Transactional
    public void updateIsOperating(DayOfWeek dayOfWeek, boolean isVacation, boolean evenWeek, LocalDateTime now) {
        List<OperatingCondition> operatingConditionList  = findOperatingCondition(dayOfWeek, isVacation, evenWeek);

        for(OperatingCondition operCondition : operatingConditionList) {
            boolean isOperating = checkOperatingTime(operCondition, now);

            if(operCondition.getBuilding() != null) {
                log.info("building: {}", operCondition.getBuilding().getName());
                operCondition.getBuilding().setOperating(isOperating);
                changeNodeIsOperating(isOperating, operCondition.getBuilding());
            }
            else if(operCondition.getClassroom() != null) {
                log.info("classroom: {}", operCondition.getClassroom().getName());
                operCondition.getClassroom().setOperating(isOperating);
            }
            else if(operCondition.getFacility() != null) {
                log.info("facility: {}", operCondition.getFacility().getName());
                operCondition.getFacility().setOperating(isOperating);
            }
        }
    }

    private List<OperatingCondition> findOperatingCondition(DayOfWeek dayOfWeek, boolean isVacation, boolean evenWeek) {
        List<OperatingCondition> operatingConditionList  = operatingConditionRepository.findAllByDayOfWeekAndIsVacation(dayOfWeek, isVacation);

        if(dayOfWeek == DayOfWeek.SATURDAY) { // 토요일인 경우
            if(evenWeek) {
                operatingConditionList.stream().filter(operatingCondition ->
                    operatingCondition.getOperatingWeekend() == OperatingWeekend.EVEN || operatingCondition.getOperatingWeekend() == OperatingWeekend.EVERY);
            }
            else {
                operatingConditionList.stream().filter(operatingCondition ->
                    operatingCondition.getOperatingWeekend() == OperatingWeekend.ODD || operatingCondition.getOperatingWeekend() == OperatingWeekend.EVERY);
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
