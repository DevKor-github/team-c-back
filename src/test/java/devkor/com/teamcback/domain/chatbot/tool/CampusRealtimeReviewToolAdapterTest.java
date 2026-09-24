package devkor.com.teamcback.domain.chatbot.tool;

import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.NO_DATA;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.UNSUPPORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import devkor.com.teamcback.domain.ble.dto.response.BLETimePatternRes;
import devkor.com.teamcback.domain.ble.dto.response.GetBLERes;
import devkor.com.teamcback.domain.ble.service.BLEService;
import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.tool.dto.CrowdLevel;
import devkor.com.teamcback.domain.chatbot.tool.dto.CrowdStatusToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetCrowdStatusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetPlaceReviewsToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.PlaceReviewsToolData;
import devkor.com.teamcback.domain.course.service.CourseService;
import devkor.com.teamcback.domain.place.service.CafeteriaMenuService;
import devkor.com.teamcback.domain.review.dto.response.GetReviewPlaceDetailRes;
import devkor.com.teamcback.domain.review.dto.response.SearchPlaceReviewRes;
import devkor.com.teamcback.domain.review.service.ReviewService;
import devkor.com.teamcback.domain.routes.service.RouteService;
import devkor.com.teamcback.domain.schoolcalendar.service.SchoolCalendarService;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceReviewTagRes;
import devkor.com.teamcback.domain.search.service.SearchService;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampusRealtimeReviewToolAdapterTest {
    @Mock SearchService searchService;
    @Mock RouteService routeService;
    @Mock CafeteriaMenuService cafeteriaMenuService;
    @Mock CourseService courseService;
    @Mock SchoolCalendarService schoolCalendarService;
    @Mock BLEService bleService;
    @Mock ReviewService reviewService;
    private CampusToolAdapter adapter;

    @BeforeEach
    void setUp() {
        ChatbotProperties properties = new ChatbotProperties(true,
                new ChatbotProperties.Llm("google", "gemini-2.5-flash-lite", 500, 8),
                new ChatbotProperties.Agent(6, 5, 60),
                new ChatbotProperties.Tools(new ChatbotProperties.Limits(5, 10),
                        new ChatbotProperties.Limits(10, 20), 7,
                        new ChatbotProperties.Limits(5, 10)),
                new ChatbotProperties.RateLimit(30, 10, 5, "Asia/Seoul"));
        adapter = new CampusToolAdapter(searchService, routeService, cafeteriaMenuService, courseService,
                schoolCalendarService, bleService, reviewService, properties);
    }

    @Test
    void mapsSensorEstimateAndOmitsPatternByDefaultWithoutDeviceMetadata() {
        GetBLERes ble = ble(1, 42, 100, LocalDateTime.of(2026, 9, 23, 12, 0));
        when(bleService.getBLE(10L)).thenReturn(ble);

        var result = adapter.getCrowdStatus(new GetCrowdStatusToolRequest(10L, null));

        assertThat(result.error()).isNull();
        assertThat(result.crowd()).satisfies(crowd -> {
            assertThat(crowd.estimatedPeople()).isEqualTo(42);
            assertThat(crowd.capacity()).isEqualTo(100);
            assertThat(crowd.level()).isEqualTo(CrowdLevel.AVAILABLE);
            assertThat(crowd.stale()).isFalse();
            assertThat(crowd.typicalPattern()).isNull();
        });
        verifyNoInteractions(reviewService);
        assertThat(recordFields(CrowdStatusToolData.class))
                .doesNotContain("id", "deviceId", "deviceName", "lastStatus", "ratio", "defaultCount");
    }

    @Test
    void staleFailureStatusIsUnknownButRetainsMeasuredEstimateAndTimestamp() {
        GetBLERes stale = ble(3, 42, 100, LocalDateTime.of(2026, 9, 23, 10, 0));
        when(bleService.getBLE(10L)).thenReturn(stale);

        var result = adapter.getCrowdStatus(new GetCrowdStatusToolRequest(10L, false));

        assertThat(result.error()).isNull();
        assertThat(result.crowd().level()).isEqualTo(CrowdLevel.UNKNOWN);
        assertThat(result.crowd().stale()).isTrue();
        assertThat(result.crowd().estimatedPeople()).isEqualTo(42);
    }

    @Test
    void noSensorOrSensorDataIsNoData() {
        when(bleService.getBLE(10L)).thenThrow(new GlobalException(ResultCode.NOT_FOUND_DEVICE));
        assertThat(adapter.getCrowdStatus(new GetCrowdStatusToolRequest(10L, false)).error().code())
                .isEqualTo(NO_DATA);

        when(bleService.getBLE(11L)).thenThrow(new GlobalException(ResultCode.NO_DATA_FOR_DEVICE));
        assertThat(adapter.getCrowdStatus(new GetCrowdStatusToolRequest(11L, false)).error().code())
                .isEqualTo(NO_DATA);
    }

    @Test
    void typicalPatternIsIncludedOnlyWhenRequested() {
        GetBLERes current = ble(0, 12, 100, LocalDateTime.now());
        when(bleService.getBLE(10L)).thenReturn(current);
        BLETimePatternRes pattern = mock(BLETimePatternRes.class);
        when(pattern.getDayOfWeeks()).thenReturn(new String[]{"mon"});
        when(pattern.getHours()).thenReturn(new int[]{7, 10});
        when(pattern.getAverages()).thenReturn(new int[][]{{10, 20}});
        when(bleService.getBLETimePattern(10L)).thenReturn(pattern);

        var result = adapter.getCrowdStatus(new GetCrowdStatusToolRequest(10L, true));

        assertThat(result.crowd().typicalPattern()).extracting(item -> item.hour())
                .containsExactly(7, 10);
        verify(bleService).getBLETimePattern(10L);
    }

    @Test
    void limitsAndMinimizesReviewsWithoutAuthorOrImageFields() {
        List<SearchPlaceReviewRes> source = List.of(
                review("첫 리뷰", true, "26.09.23(화)"), review("둘째 리뷰", false, "26.09.22(월)"),
                review("셋째 리뷰", false, "26.09.21(일)"), review("넷째 리뷰", false, "26.09.20(토)"),
                review("다섯째 리뷰", false, "26.09.19(금)"), review("여섯째 리뷰", false, "26.09.18(목)"));
        GetReviewPlaceDetailRes response = reviewResponse(source, "4.25");
        when(reviewService.getReviewPlaceDetail(20L)).thenReturn(response);

        var result = adapter.getPlaceReviews(new GetPlaceReviewsToolRequest(20L, null));

        assertThat(result.error()).isNull();
        assertThat(result.reviews().averageRating()).isEqualTo(4.25);
        assertThat(result.reviews().topTags()).extracting(tag -> tag.tag()).containsExactly("맛있어요");
        assertThat(result.reviews().reviews()).hasSize(5);
        assertThat(result.reviews().truncated()).isTrue();
        assertThat(recordFields(PlaceReviewsToolData.class)).doesNotContain("userId", "username", "nickname",
                "profileImageUrl", "characterImage", "reviewImage", "reviewId", "fileUuid");
        assertThat(recordFields(devkor.com.teamcback.domain.chatbot.tool.dto.ReviewSummary.class))
                .containsExactlyInAnyOrder("comment", "revisit", "createdAt")
                .doesNotContain("userId", "username", "profileImageUrl", "imageUrl");
    }

    @Test
    void maxReviewLimitAndUnsupportedOrEmptyReviewsHaveStableErrors() {
        List<SearchPlaceReviewRes> source = java.util.stream.IntStream.range(0, 12)
                .mapToObj(index -> review("리뷰" + index, false, "26.09.23(화)")).toList();
        GetReviewPlaceDetailRes response = reviewResponse(source, "NaN");
        when(reviewService.getReviewPlaceDetail(20L)).thenReturn(response);
        var limited = adapter.getPlaceReviews(new GetPlaceReviewsToolRequest(20L, 99));
        assertThat(limited.reviews().reviews()).hasSize(10);
        assertThat(limited.reviews().averageRating()).isNull();

        GetReviewPlaceDetailRes emptyResponse = reviewResponse(List.of(), "NaN");
        when(reviewService.getReviewPlaceDetail(21L)).thenReturn(emptyResponse);
        assertThat(adapter.getPlaceReviews(new GetPlaceReviewsToolRequest(21L, null)).error().code())
                .isEqualTo(NO_DATA);

        when(reviewService.getReviewPlaceDetail(22L))
                .thenThrow(new GlobalException(ResultCode.NOT_SUPPORTED_PLACE_TYPE));
        assertThat(adapter.getPlaceReviews(new GetPlaceReviewsToolRequest(22L, null)).error().code())
                .isEqualTo(UNSUPPORTED);
    }

    private GetBLERes ble(int status, int people, int capacity, LocalDateTime measuredAt) {
        GetBLERes response = mock(GetBLERes.class);
        lenient().when(response.getPlaceId()).thenReturn(10L);
        lenient().when(response.getLastStatus()).thenReturn(status);
        lenient().when(response.getLastCount()).thenReturn(people);
        lenient().when(response.getCapacity()).thenReturn(capacity);
        lenient().when(response.getLastTime()).thenReturn(measuredAt);
        return response;
    }

    private GetReviewPlaceDetailRes reviewResponse(List<SearchPlaceReviewRes> reviews, String average) {
        GetReviewPlaceDetailRes response = mock(GetReviewPlaceDetailRes.class);
        lenient().when(response.getPlaceId()).thenReturn(20L);
        lenient().when(response.getName()).thenReturn("하나스퀘어 카페");
        lenient().when(response.getStarAverage()).thenReturn(average);
        lenient().when(response.getTagList()).thenReturn(List.of(new SearchPlaceReviewTagRes(1L, "맛있어요", 3)));
        lenient().when(response.getReviewList()).thenReturn(reviews);
        return response;
    }

    private SearchPlaceReviewRes review(String comment, boolean revisit, String createdAt) {
        SearchPlaceReviewRes response = mock(SearchPlaceReviewRes.class);
        lenient().when(response.getComment()).thenReturn(comment);
        lenient().when(response.isRevisit()).thenReturn(revisit);
        lenient().when(response.getCreatedAt()).thenReturn(createdAt);
        return response;
    }

    private List<String> recordFields(Class<?> type) {
        return java.util.Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}
