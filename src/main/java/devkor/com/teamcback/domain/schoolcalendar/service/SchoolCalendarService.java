package devkor.com.teamcback.domain.schoolcalendar.service;

import devkor.com.teamcback.domain.schoolcalendar.dto.response.GetVacationRes;
import devkor.com.teamcback.domain.schoolcalendar.entity.SchoolCalendar;
import devkor.com.teamcback.domain.schoolcalendar.repository.SchoolCalendarRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_SCHOOL_CALENDAR;

@Service
@RequiredArgsConstructor
public class SchoolCalendarService {
    private final SchoolCalendarRepository schoolCalendarRepository;

    /**
     * 방학 여부 반환
     */
    public GetVacationRes isVacation() {
        SchoolCalendar schoolCalendar = findSchoolCalendar(1L);
        return new GetVacationRes(schoolCalendar);
    }

    private SchoolCalendar findSchoolCalendar(Long id) {
        return schoolCalendarRepository.findById(id).orElseThrow(() -> new GlobalException(NOT_FOUND_SCHOOL_CALENDAR));
    }

    /**
     * 방학 여부 반환
     */
    public boolean isVacationTf() {
        SchoolCalendar schoolCalendar = schoolCalendarRepository.findById(1L).orElseThrow(() -> new GlobalException(NOT_FOUND_SCHOOL_CALENDAR));
        return schoolCalendar.isActive();
    }

}
