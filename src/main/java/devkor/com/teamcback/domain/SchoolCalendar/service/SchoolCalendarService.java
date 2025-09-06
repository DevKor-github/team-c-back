package devkor.com.teamcback.domain.SchoolCalendar.service;

import devkor.com.teamcback.domain.SchoolCalendar.dto.response.GetVacationRes;
import devkor.com.teamcback.domain.SchoolCalendar.entity.SchoolCalendar;
import devkor.com.teamcback.domain.SchoolCalendar.repository.SchoolCalendarRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_SCHOOL_CALENDAR;

@Service
@RequiredArgsConstructor
public class SchoolCalendarService {
    private final SchoolCalendarRepository schoolCalendarRepository;

    public GetVacationRes isVacation() {
        SchoolCalendar schoolCalendar = findSchoolCalendar(1L);
        return new GetVacationRes(schoolCalendar);
    }

    /**
     * 방학 여부 반환
     */
    private SchoolCalendar findSchoolCalendar(Long id) {
        return schoolCalendarRepository.findById(id).orElseThrow(() -> new GlobalException(NOT_FOUND_SCHOOL_CALENDAR));
    }

}
