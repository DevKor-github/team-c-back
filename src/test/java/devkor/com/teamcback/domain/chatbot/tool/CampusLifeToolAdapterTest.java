package devkor.com.teamcback.domain.chatbot.tool;

import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.INVALID_INPUT;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.NO_DATA;
import static devkor.com.teamcback.domain.common.entity.Weekday.MON;
import static devkor.com.teamcback.domain.common.entity.Weekday.TUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.tool.dto.CafeteriaMenuDay;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCafeteriaMenuToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCafeteriaMenuToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetRoomCoursesToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetRoomCoursesToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.RoomCourseToolItem;
import devkor.com.teamcback.domain.course.dto.response.GetCourseListRes;
import devkor.com.teamcback.domain.course.dto.response.GetCourseRes;
import devkor.com.teamcback.domain.course.service.CourseService;
import devkor.com.teamcback.domain.place.dto.response.GetCafeteriaMenuListRes;
import devkor.com.teamcback.domain.place.service.CafeteriaMenuService;
import devkor.com.teamcback.domain.routes.service.RouteService;
import devkor.com.teamcback.domain.schoolcalendar.dto.response.GetSchoolCalendarRes;
import devkor.com.teamcback.domain.schoolcalendar.dto.response.GetSchoolCalendarTermRes;
import devkor.com.teamcback.domain.schoolcalendar.service.SchoolCalendarService;
import devkor.com.teamcback.domain.search.service.SearchService;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampusLifeToolAdapterTest {
    @Mock SearchService searchService;
    @Mock RouteService routeService;
    @Mock CafeteriaMenuService cafeteriaMenuService;
    @Mock CourseService courseService;
    @Mock SchoolCalendarService schoolCalendarService;
    private CampusToolAdapter adapter;

    @BeforeEach
    void setUp() {
        ChatbotProperties properties = new ChatbotProperties(true,
                new ChatbotProperties.Llm("google", "gemini-2.5-flash-lite", 500, 8),
                new ChatbotProperties.Agent(6, 5, 60, 15),
                new ChatbotProperties.Tools(new ChatbotProperties.Limits(5, 10),
                        new ChatbotProperties.Limits(10, 20), 7,
                        new ChatbotProperties.Limits(5, 10)),
                new ChatbotProperties.RateLimit(30, 10, 5, "Asia/Seoul"));
        adapter = new CampusToolAdapter(searchService, routeService, cafeteriaMenuService,
                courseService, schoolCalendarService, null, null, properties);
    }

    @Test
    void omittedMenuEndDateQueriesOneInclusiveDayAndReturnsCompactOutput() {
        LocalDate date = LocalDate.of(2026, 9, 23);
        Map<LocalDate, Map<String, String>> menus = Map.of(date, Map.of("LUNCH", "비빔밥"));
        when(cafeteriaMenuService.getCafeteriaMenu(10L, date, date.plusDays(1)))
                .thenReturn(new GetCafeteriaMenuListRes(10L, "학생회관 식당", "address", "hours", "phone", menus));

        var result = adapter.getCafeteriaMenu(new GetCafeteriaMenuToolRequest(10L, date, null));

        assertThat(result.error()).isNull();
        assertThat(result.cafeteria().placeId()).isEqualTo(10L);
        assertThat(result.cafeteria().placeName()).isEqualTo("학생회관 식당");
        assertThat(result.cafeteria().days()).singleElement().satisfies(day -> {
            assertThat(day.date()).isEqualTo(date);
            assertThat(day.meals()).singleElement().satisfies(meal -> {
                assertThat(meal.mealType()).isEqualTo("LUNCH");
                assertThat(meal.menu()).isEqualTo("비빔밥");
            });
        });
        assertThat(recordFields(GetCafeteriaMenuToolData.class))
                .containsExactlyInAnyOrder("placeId", "placeName", "days")
                .doesNotContain("address", "contact", "imageUrl", "operatingTime");
        assertThat(recordFields(CafeteriaMenuDay.class)).doesNotContain("id", "placeId");
    }

    @Test
    void menuRangeIsInclusiveAndDaysAreSorted() {
        LocalDate start = LocalDate.of(2026, 9, 21);
        LocalDate end = start.plusDays(2);
        Map<LocalDate, Map<String, String>> menus = new HashMap<>();
        menus.put(end, Map.of("DINNER", "국수"));
        menus.put(start, Map.of("LUNCH", "백반"));
        when(cafeteriaMenuService.getCafeteriaMenu(10L, start, end.plusDays(1)))
                .thenReturn(new GetCafeteriaMenuListRes(10L, "식당", "", "", "", menus));

        var result = adapter.getCafeteriaMenu(new GetCafeteriaMenuToolRequest(10L, start, end));

        assertThat(result.cafeteria().days()).extracting(CafeteriaMenuDay::date)
                .containsExactly(start, end);
    }

    @Test
    void invalidOrOverSevenDayMenuRangeIsRejectedBeforeServiceCall() {
        LocalDate start = LocalDate.of(2026, 9, 1);

        assertThat(adapter.getCafeteriaMenu(new GetCafeteriaMenuToolRequest(10L, start, start.minusDays(1)))
                .error().code()).isEqualTo(INVALID_INPUT);
        assertThat(adapter.getCafeteriaMenu(new GetCafeteriaMenuToolRequest(10L, start, start.plusDays(7)))
                .error().code()).isEqualTo(INVALID_INPUT);
        verifyNoInteractions(cafeteriaMenuService);
    }

    @Test
    void emptyMenuMapsToNoData() {
        LocalDate date = LocalDate.of(2026, 9, 23);
        when(cafeteriaMenuService.getCafeteriaMenu(10L, date, date.plusDays(1)))
                .thenReturn(new GetCafeteriaMenuListRes(10L, "식당", "", "", "", Map.of(date, Map.of())));

        var result = adapter.getCafeteriaMenu(new GetCafeteriaMenuToolRequest(10L, date, date));

        assertThat(result.cafeteria()).isNull();
        assertThat(result.error().code()).isEqualTo(NO_DATA);
    }

    @Test
    void roomCoursesFilterWeekdayAndMergeOnlyConsecutiveSameCourseRows() {
        GetCourseRes math2 = course(1L, "수학", "김교수", "MATH101", "01", "MON", 2);
        GetCourseRes math3 = course(1L, "수학", "김교수", "MATH101", "01", "MON", 3);
        GetCourseRes math5 = course(1L, "수학", "김교수", "MATH101", "01", "MON", 5);
        GetCourseRes other3 = course(2L, "수학", "이교수", "MATH102", "02", "MON", 3);
        GetCourseRes tuesday = course(3L, "영어", "박교수", "ENG101", "01", "TUE", 1);
        when(courseService.getCourseList(20L)).thenReturn(new GetCourseListRes(20L, "과도관 101호",
                Map.of(MON, List.of(math2, math3, other3, math5), TUE, List.of(tuesday))));

        var result = adapter.getRoomCourses(new GetRoomCoursesToolRequest(20L, MON));

        assertThat(result.error()).isNull();
        assertThat(result.room().roomName()).isEqualTo("과도관 101호");
        assertThat(result.room().year()).isEqualTo(2026);
        assertThat(result.room().term()).isEqualTo("FALL");
        assertThat(result.room().courses()).hasSize(3);
        assertThat(result.room().courses().get(0))
                .extracting(RoomCourseToolItem::courseCode, RoomCourseToolItem::startPeriod,
                        RoomCourseToolItem::endPeriod)
                .containsExactly("MATH101", 2, 3);
        assertThat(result.room().courses()).extracting(RoomCourseToolItem::weekday).containsOnly(MON);
        assertThat(recordFields(GetRoomCoursesToolData.class))
                .doesNotContain("courseId", "studentId", "userId", "department", "unit", "type");
    }

    @Test
    void noCoursesForRequestedWeekdayMapsToNoData() {
        GetCourseRes monday = course(1L, "수학", "김교수", "MATH101", "01", "MON", 2);
        when(courseService.getCourseList(20L)).thenReturn(new GetCourseListRes(20L, "과도관 101호",
                Map.of(MON, List.of(monday))));

        var result = adapter.getRoomCourses(new GetRoomCoursesToolRequest(20L, TUE));

        assertThat(result.room()).isNull();
        assertThat(result.error().code()).isEqualTo(NO_DATA);
    }

    @Test
    void campusStatusCombinesCurrentDomainValues() {
        GetSchoolCalendarTermRes term = mock(GetSchoolCalendarTermRes.class);
        GetSchoolCalendarRes vacation = mock(GetSchoolCalendarRes.class);
        GetSchoolCalendarRes koyeon = mock(GetSchoolCalendarRes.class);
        when(term.getTerm()).thenReturn("FALL");
        when(vacation.isActive()).thenReturn(false);
        when(koyeon.isActive()).thenReturn(true);
        when(schoolCalendarService.getTerm()).thenReturn(term);
        when(schoolCalendarService.isVacation()).thenReturn(vacation);
        when(schoolCalendarService.isKoyeon()).thenReturn(koyeon);

        var result = adapter.getCampusStatus();

        assertThat(result.term()).isEqualTo("FALL");
        assertThat(result.vacation()).isFalse();
        assertThat(result.koyeonPeriod()).isTrue();
        assertThat(result.error()).isNull();
        verify(schoolCalendarService).getTerm();
        verify(schoolCalendarService).isVacation();
        verify(schoolCalendarService).isKoyeon();
    }

    private GetCourseRes course(Long id, String subject, String professor, String code,
                                String section, String weekday, int period) {
        GetCourseRes row = mock(GetCourseRes.class);
        lenient().when(row.getCourseId()).thenReturn(id);
        lenient().when(row.getYear()).thenReturn(2026);
        lenient().when(row.getTerm()).thenReturn("FALL");
        lenient().when(row.getSubject()).thenReturn(subject);
        lenient().when(row.getProfessor()).thenReturn(professor);
        lenient().when(row.getCode()).thenReturn(code);
        lenient().when(row.getSection()).thenReturn(section);
        lenient().when(row.getWeekday()).thenReturn(weekday);
        lenient().when(row.getClassTime()).thenReturn(period);
        return row;
    }

    private List<String> recordFields(Class<?> type) {
        return java.util.Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}
