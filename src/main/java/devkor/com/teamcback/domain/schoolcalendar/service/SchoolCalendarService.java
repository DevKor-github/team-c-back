package devkor.com.teamcback.domain.schoolcalendar.service;

import devkor.com.teamcback.domain.schoolcalendar.dto.response.GetSchoolCalendarRes;
import devkor.com.teamcback.domain.schoolcalendar.dto.response.UpdateSchoolCalendarRes;
import devkor.com.teamcback.domain.schoolcalendar.entity.SchoolCalendar;
import devkor.com.teamcback.domain.schoolcalendar.repository.SchoolCalendarRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import jakarta.transaction.Transactional;
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
    public GetSchoolCalendarRes isVacation() {
        return new GetSchoolCalendarRes(findSchoolCalendar(1L));
    }

    /**
     * 고연전 여부 반환
     */
    public GetSchoolCalendarRes isKoyeon() {
        return new GetSchoolCalendarRes(findSchoolCalendar(2L));
    }

    /**
     * 방학 여부 수정(토글)
     */
    @Transactional
    public UpdateSchoolCalendarRes updateVacationActive() {
        updateSchoolCalendarActive(1L);
        return new UpdateSchoolCalendarRes();
    }

    /**
     * 고연전 여부 수정(토글)
     */
    @Transactional
    public UpdateSchoolCalendarRes updateKoyeonActive() {
        updateSchoolCalendarActive(2L);
        return new UpdateSchoolCalendarRes();
    }

    /**
     * 방학 여부 반환
     */
    public boolean isVacationTf() {
        SchoolCalendar schoolCalendar = schoolCalendarRepository.findById(1L).orElseThrow(() -> new GlobalException(NOT_FOUND_SCHOOL_CALENDAR));
        return schoolCalendar.isActive();
    }

    /**
     * 학교 일정 진행 여부 수정
     */
    private void updateSchoolCalendarActive(long id) {
        SchoolCalendar schoolCalendar = findSchoolCalendar(id);
        schoolCalendar.setActive(!schoolCalendar.isActive()); // 수정
    }

    private SchoolCalendar findSchoolCalendar(Long id) {
        return schoolCalendarRepository.findById(id).orElseThrow(() -> new GlobalException(NOT_FOUND_SCHOOL_CALENDAR));
    }

}
