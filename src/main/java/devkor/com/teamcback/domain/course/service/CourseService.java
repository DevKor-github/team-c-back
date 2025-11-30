package devkor.com.teamcback.domain.course.service;

import devkor.com.teamcback.domain.common.entity.Weekday;
import devkor.com.teamcback.domain.course.dto.response.GetCourseListRes;
import devkor.com.teamcback.domain.course.dto.response.GetCourseRes;
import devkor.com.teamcback.domain.course.entity.CourseDetail;
import devkor.com.teamcback.domain.course.entity.Term;
import devkor.com.teamcback.domain.course.repository.CourseDetailRepository;
import devkor.com.teamcback.domain.course.repository.CourseRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.schoolcalendar.repository.SchoolCalendarRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final PlaceRepository placeRepository;
    private final CourseRepository courseRepository;
    private final CourseDetailRepository courseDetailRepository;
    private final SchoolCalendarRepository schoolCalendarRepository;

    public GetCourseListRes getCourseList(Long placeId) {

        int year = Year.now().getValue();
        Term term = getTerm();

        Place place = getPlaceById(placeId);

        List<CourseDetail> courseDetailList = courseDetailRepository.findByPlaceId(placeId);

        Map<Weekday, List<GetCourseRes>> map = new LinkedHashMap<>();
        for(Weekday weekday : Weekday.values()) {
            map.put(weekday, new ArrayList<>());
            for(CourseDetail courseDetail : courseDetailList.stream().filter(courseDetail -> courseDetail.getWeekday() == weekday).sorted(Comparator.comparing(CourseDetail::getStart)).toList()) {
                if(courseDetail.getCourse().getYear() == year && courseDetail.getCourse().getTerm() == term) {
                    for(int i = courseDetail.getStart(); i <= courseDetail.getEnd(); i++) {
                        map.get(weekday).add(new GetCourseRes(courseDetail.getCourse(), weekday.name(), i));
                    }
                }
            }
        }

        return new GetCourseListRes(placeId, place.getBuilding().getName() + " " + place.getName(), map);
    }

    private Term getTerm() {
        return schoolCalendarRepository.findById(3L).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_SCHOOL_CALENDAR)).getTerm();
    }

    private Place getPlaceById(Long id) {
        return placeRepository.findById(id).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_PLACE));
    }

/*    private Course getCourseById(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_COURSE));
    }*/
}
