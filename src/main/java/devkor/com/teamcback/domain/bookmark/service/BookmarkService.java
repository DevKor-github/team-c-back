package devkor.com.teamcback.domain.bookmark.service;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateBookmarkReq;
import devkor.com.teamcback.domain.bookmark.dto.request.ModifyBookmarkReq;
import devkor.com.teamcback.domain.bookmark.dto.response.CreateBookmarkRes;
import devkor.com.teamcback.domain.bookmark.dto.response.DeleteBookmarkRes;
import devkor.com.teamcback.domain.bookmark.dto.response.ModifyBookmarkRes;
import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.entity.CategoryBookmark;
import devkor.com.teamcback.domain.bookmark.entity.UserBookmarkLog;
import devkor.com.teamcback.domain.bookmark.repository.BookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryBookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.bookmark.repository.UserBookmarkLogRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.annotation.UpdateScore;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final BuildingRepository buildingRepository;
    private final PlaceRepository placeRepository;
    private final UserBookmarkLogRepository userBookmarkLogRepository;
    private final CategoryBookmarkRepository categoryBookmarkRepository;

    /**
     * 즐겨찾기 업데이트
     */
    @Transactional
    @UpdateScore(addScore = 1)
    public CreateBookmarkRes createBookmark(Long userId, CreateBookmarkReq req) {
        User user = findUser(userId);

        // placeType & Id로 장소 존재 여부 확인
        checkPlaceExists(req.getLocationType(), req.getLocationId());

        // 사용자의 카테고리 목록
        List<Category> userCategoryList = categoryRepository.findAllByUser(user);

        // 어떤 카테고리도 선택하지 않았을 때 해당 장소에 대한 사용자의 즐겨찾기 삭제
        if(req.getCategoryIdList().isEmpty()) {
            Bookmark bookmark = bookmarkRepository.findByLocationIdAndLocationTypeAndCategoryBookmarkList_CategoryIn(req.getLocationId(), req.getLocationType(), userCategoryList);
            if(bookmark != null) {
                bookmarkRepository.delete(bookmark);
            }
        }

        else {
            // 자신의 카테고리가 아닌 id로 요청했을 경우
            for (Long selectCategoryId : req.getCategoryIdList()) {
                if (!userCategoryList.stream().map(Category::getId).toList()
                    .contains(selectCategoryId)) {
                    throw new GlobalException(FORBIDDEN);
                }
            }

            for (Category category : userCategoryList) {
                boolean isSelected = req.getCategoryIdList().contains(category.getId());
                Bookmark existedBookmark = bookmarkRepository.findByLocationIdAndLocationTypeAndCategoryBookmarkList_Category(
                    req.getLocationId(), req.getLocationType(), category);

                if (existedBookmark != null && !isSelected) { // 북마크 o -> 선택 x
                    // 해당 카테고리 내의 즐겨찾기 삭제
                    CategoryBookmark categoryBookmark = categoryBookmarkRepository.findByCategoryAndBookmark(
                            category, existedBookmark)
                        .orElseThrow(() -> new GlobalException(NOT_FOUND_IN_CATEGORY));
                    deleteCategoryBookmark(categoryBookmark);
                } else if (existedBookmark == null && isSelected) { // 북마크 x -> 선택 o
                    // 해당 카테고리의 북마크 생성
                    Bookmark bookmark = bookmarkRepository.findByLocationIdAndLocationTypeAndCategoryBookmarkList_CategoryIn(req.getLocationId(), req.getLocationType(), userCategoryList);
                    if(bookmark == null) {
                        bookmark = bookmarkRepository.save(new Bookmark(req));
                    }

                    CategoryBookmark categoryBookmark = new CategoryBookmark();
                    categoryBookmark.setCategoryAndBookmark(category, bookmark);
                    categoryBookmarkRepository.save(categoryBookmark);
                }

            }

            // 첫 저장이면 log 추가 & Score 증가
            if(!userBookmarkLogRepository.existsByUserAndLocationIdAndLocationType(user, req.getLocationId(), req.getLocationType())) {
                userBookmarkLogRepository.save(new UserBookmarkLog(req.getLocationId(), req.getLocationType(), user));
                log.info("북마크 로그 추가");
            }
        }

        return new CreateBookmarkRes();
    }

    /**
     * 즐겨찾기 삭제 - 해당 장소와 관련된 모든 즐겨찾기 삭제
     */
    @Transactional
    public DeleteBookmarkRes deleteBookmark(Long userId, Long bookmarkId) {
        User user = findUser(userId);

        Bookmark bookmark = findBookmark(bookmarkId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, bookmark);

        bookmarkRepository.delete(bookmark);

        return new DeleteBookmarkRes();
    }

    /**
     * 즐겨찾기 수정 (memo 수정만 가능)
     */
    @Transactional
    public ModifyBookmarkRes modifyBookmark(Long userId, Long bookmarkId, ModifyBookmarkReq req) {
        User user = findUser(userId);
        Bookmark bookmark = findBookmark(bookmarkId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, bookmark);

        bookmark.update(req.getMemo());

        return new ModifyBookmarkRes();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }

    private Bookmark findBookmark(Long bookmarkId) {
        return bookmarkRepository.findById(bookmarkId).orElseThrow(() -> new GlobalException(NOT_FOUND_BOOKMARK));
    }

    private void checkAuthority(User user, Bookmark bookmark) {
        if(bookmark.getCategoryBookmarkList().stream().noneMatch(categoryBookmark -> categoryBookmark.getCategory().getUser().getUserId().equals(user.getUserId()))) {
            throw new GlobalException(FORBIDDEN);
        }
    }

    private void checkPlaceDuplication(Category category, LocationType locationType, Long locationId) {
        CategoryBookmark categoryBookmark = categoryBookmarkRepository.findByCategoryAndBookmarkLocationIdAndBookmarkLocationType(category, locationId, locationType);

        // 같은 카테고리에 동일 북마크가 존재하는 경우
        if (categoryBookmark != null) {
            throw new GlobalException(DUPLICATED_BOOKMARK);
        }
    }

    private void checkPlaceExists(LocationType locationType, Long locationId) {
        if (LocationType.BUILDING.equals(locationType) && !buildingRepository.existsById(locationId)) {
            throw new GlobalException(NOT_FOUND_BUILDING);
        } else if (LocationType.PLACE.equals(locationType) && !placeRepository.existsById(locationId)) {
            throw new GlobalException(NOT_FOUND_PLACE);
        }
    }

    private void deleteCategoryBookmark(CategoryBookmark categoryBookmark) {
        Bookmark bookmark = categoryBookmark.getBookmark();
        categoryBookmark.remove();
        categoryBookmarkRepository.delete(categoryBookmark);
        if(bookmark.getCategoryBookmarkList().isEmpty()) {
            bookmarkRepository.delete(bookmark);
        }
    }
}
