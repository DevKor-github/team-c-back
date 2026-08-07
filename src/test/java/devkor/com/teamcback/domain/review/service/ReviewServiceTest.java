package devkor.com.teamcback.domain.review.service;

import devkor.com.teamcback.domain.character.repository.CharacterRepository;
import devkor.com.teamcback.domain.common.repository.FileRepository;
import devkor.com.teamcback.domain.common.util.FileUtil;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.review.dto.request.CreateReviewReq;
import devkor.com.teamcback.domain.review.dto.request.ModifyReviewReq;
import devkor.com.teamcback.domain.review.entity.Review;
import devkor.com.teamcback.domain.review.repository.PlaceReviewTagMapRepository;
import devkor.com.teamcback.domain.review.repository.ReviewRepository;
import devkor.com.teamcback.domain.review.repository.ReviewTagMapRepository;
import devkor.com.teamcback.domain.review.repository.ReviewTagRepository;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private PlaceRepository placeRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewTagRepository reviewTagRepository;
    @Mock private ReviewTagMapRepository reviewTagMapRepository;
    @Mock private PlaceReviewTagMapRepository placeReviewTagMapRepository;
    @Mock private FileRepository fileRepository;
    @Mock private UserRepository userRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private FileUtil fileUtil;

    private ReviewService reviewService;
    private User user;
    private Place place;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                placeRepository,
                reviewRepository,
                reviewTagRepository,
                reviewTagMapRepository,
                placeReviewTagMapRepository,
                fileRepository,
                userRepository,
                characterRepository,
                fileUtil
        );

        user = new User("reviewer", "reviewer@test.com", Role.USER, Provider.KAKAO);
        ReflectionTestUtils.setField(user, "userId", 7L);
        place = new Place();
        ReflectionTestUtils.setField(place, "id", 9562L);
    }

    @Test
    void createsReviewWithCommentShorterThanTenCharacters() {
        CreateReviewReq req = new CreateReviewReq();
        req.setScore(5.0);
        req.setComment("좋아요");

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(placeRepository.findById(9562L)).thenReturn(Optional.of(place));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 1L);
            return review;
        });

        reviewService.createReview(7L, 9562L, req);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getComment()).isEqualTo("좋아요");
    }

    @Test
    void modifiesReviewWithCommentShorterThanTenCharacters() {
        CreateReviewReq createReq = new CreateReviewReq();
        createReq.setScore(4.0);
        createReq.setComment("기존 한줄평입니다");
        Review review = new Review(createReq, user, place);

        ModifyReviewReq modifyReq = new ModifyReviewReq();
        modifyReq.setScore(4.5);
        modifyReq.setComment("괜찮음");

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        reviewService.modifyReview(7L, 1L, modifyReq);

        assertThat(review.getComment()).isEqualTo("괜찮음");
    }
}
