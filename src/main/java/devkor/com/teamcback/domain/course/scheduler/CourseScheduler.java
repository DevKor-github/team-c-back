package devkor.com.teamcback.domain.course.scheduler;

import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.course.entity.CourseDetail;
import devkor.com.teamcback.domain.course.repository.CourseDetailRepository;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchListRes;
import devkor.com.teamcback.domain.search.service.SearchService;
import devkor.com.teamcback.global.redis.RedisLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j(topic = "Course Scheduler")
@Component
@RequiredArgsConstructor
public class CourseScheduler {
    private final RedisLockUtil redisLockUtil;
    private final CourseDetailRepository courseDetailRepository;
    private final PlaceRepository placeRepository;
    private final SearchService searchService;

    //@EventListener(ApplicationReadyEvent.class)
    //@Transactional
    public void updateCourseDetails() {
        redisLockUtil.executeWithLock("course_lock", 1, 300, () -> {

            log.info("강의 장소 업데이트");

            List<CourseDetail> courseDetails = courseDetailRepository.findLimitByPlaceIdIsNull(500);

            for (CourseDetail courseDetail : courseDetails) {
                String placeName = courseDetail.getPlaceName();
                if(placeName.equals("장소정보없음")) continue;

                GlobalSearchListRes resList = searchService.globalSearch(placeName, null);

                if(!resList.getList().isEmpty() && resList.getList().get(0).getLocationType() == LocationType.PLACE) {
                    Place place = placeRepository.findById(resList.getList().get(0).getId()).orElseThrow();
                    courseDetail.setPlace(place);
                }

            }
            return null;
        });

    }
}
