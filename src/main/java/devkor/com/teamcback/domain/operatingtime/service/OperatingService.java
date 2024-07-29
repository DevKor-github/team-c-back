package devkor.com.teamcback.domain.operatingtime.service;

import devkor.com.teamcback.domain.operatingtime.entity.OperatingCondition;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingTime;
import devkor.com.teamcback.domain.operatingtime.entity.OperatingWeekend;
import devkor.com.teamcback.domain.operatingtime.repositoy.OperatingConditionRepository;
import devkor.com.teamcback.domain.operatingtime.repositoy.OperatingTimeRepository;
import java.time.LocalDateTime;
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

    @Transactional
    public void updateOperatingTime(boolean isWeekday, boolean isVacation, Boolean evenWeek, LocalDateTime now) {
        List<OperatingCondition> operatingConditionList  = operatingConditionRepository.findAllByIsWeekdayAndIsVacation(isWeekday, isVacation);

        if(evenWeek != null) { // 토요일인 경우
            if(evenWeek) {
                operatingConditionList.stream().filter(operatingCondition ->
                    operatingCondition.getOperatingWeekend() == OperatingWeekend.EVEN || operatingCondition.getOperatingWeekend() == OperatingWeekend.EVERY);
            }
            else {
                operatingConditionList.stream().filter(operatingCondition ->
                    operatingCondition.getOperatingWeekend() == OperatingWeekend.ODD || operatingCondition.getOperatingWeekend() == OperatingWeekend.EVERY);
            }
        }

        for(OperatingCondition operCondition : operatingConditionList) {
            boolean isOperating = checkOperatingTime(operCondition, now);
            if(operCondition.getBuilding() != null) {
                log.info("building: {}", operCondition.getBuilding().getName());
                operCondition.getBuilding().setOperating(isOperating);
//                changeNodeRouting(isOperating, operCondition.getBuilding());
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

    // 운영 시간 확인
    private boolean checkOperatingTime(OperatingCondition operatingCondition, LocalDateTime now) {
        int hour = now.getHour();
        int minute = now.getMinute();
        log.info("hour: {}", hour);
        log.info("minute: {}", minute);

        List<OperatingTime> operatingTimeList = operatingTimeRepository.findAllByOperatingCondition(operatingCondition);
        for(OperatingTime operatingTime : operatingTimeList) {
            if(hour > operatingTime.getStartHour() && hour < operatingTime.getEndHour()) {
                return true;
            }
            else if(hour == operatingTime.getStartHour() && minute >= operatingTime.getStartMinute()) {
                return true;
            }
            else if(hour == operatingTime.getEndHour() && minute <= operatingTime.getEndMinute()) {
                return true;
            }
        }
        return false;
    }

//    // 건물 운영 여부에 따라 출입문 routing 변경
//    private void changeNodeRouting(boolean isOperating, Building building) {
//        log.info("건물 출입문 routing 변경");
//        List<Node> nodeList = nodeRepository.findAllByBuildingAndNodeType(building, ENTRANCE);
//        for(Node node : nodeList) {
//            node.setRouting(isOperating);
//        }
//    }
}
