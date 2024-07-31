package devkor.com.teamcback.domain.bookmark.service;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateBookmarkReq;
import devkor.com.teamcback.domain.bookmark.dto.request.CreateCategoryReq;
import devkor.com.teamcback.domain.bookmark.dto.request.ModifyBookmarkReq;
import devkor.com.teamcback.domain.bookmark.dto.request.ModifyCategoryReq;
import devkor.com.teamcback.domain.bookmark.dto.response.*;
import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.repository.BookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.classroom.repository.ClassroomRepository;
import devkor.com.teamcback.domain.common.PlaceType;
import devkor.com.teamcback.domain.facility.repository.FacilityRepository;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final BuildingRepository buildingRepository;
    private final ClassroomRepository classroomRepository;
    private final FacilityRepository facilityRepository;

    /**
     * 카테고리 생성
     */
    @Transactional
    public CreateCategoryRes createCategory(Long userId, CreateCategoryReq req) {
        User user = findUser(userId);

        Category category = categoryRepository.save(new Category(req, user));

        return new CreateCategoryRes(category);
    }

    /**
     * 카테고리 삭제
     */
    @Transactional
    public DeleteCategoryRes deleteCategory(Long userId, Long categoryId) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, category.getUser());

        categoryRepository.delete(category);

        return new DeleteCategoryRes();
    }

    /**
     * 카테고리 수정
     */
    @Transactional
    public ModifyCategoryRes modifyCategory(Long userId, Long categoryId, ModifyCategoryReq req) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, category.getUser());

        category.update(req);

        return new ModifyCategoryRes();
    }

    /**
     * 카테고리 조회
     */
    @Transactional(readOnly = true)
    public GetCategoryRes getCategory(Long userId, Long categoryId) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, category.getUser());

        return new GetCategoryRes(category);
    }

    /**
     * 카테고리 전체 조회
     */
    @Transactional(readOnly = true)
    public List<GetCategoryRes> getAllCategories(Long userId) {
        User user = findUser(userId);

        return categoryRepository.findAllByUser(user).stream().map(category -> new GetCategoryRes(category, bookmarkRepository.countAllByCategory(category))).collect(Collectors.toList());
    }

    /**
     * 특정 카테고리 상세조회 (즐겨찾기 List)
     */
    @Transactional(readOnly = true)
    public List<GetBookmarkRes> getAllBookmarks(Long userId, Long categoryId) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, category.getUser());

        return bookmarkRepository.findAllByCategory(category).stream().map(GetBookmarkRes::new).collect(Collectors.toList());
    }

    /**
     * 즐겨찾기 생성
     */
    @Transactional
    public CreateBookmarkRes createBookmark(Long userId, Long categoryId, CreateBookmarkReq req) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);

        //카테고리 소유자 확인
        checkAuthority(user, category.getUser());

        //placeType & Id로 장소 존재 여부 확인
        checkPlaceExists(req.getPlaceType(), req.getPlaceId());

        //장소 중복 확인하기
        //해당 카테고리안에 placeType, placeId가 모두 동일한 것이 있으면 중복
        checkPlaceDuplication(category, req.getPlaceType(), req.getPlaceId());

        //즐겨찾기 생성
        Bookmark bookmark = bookmarkRepository.save(new Bookmark(req, category));
        return new CreateBookmarkRes(bookmark);
    }

    /**
     * 즐겨찾기 삭제
     */
    @Transactional
    public DeleteBookmarkRes deleteBookmark(Long userId, Long categoryId, Long bookmarkId) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);

        //해당 카테고리에 해당 북마크가 존재하는지 확인 후 가져오기
        Bookmark bookmark = findBookmark(category, bookmarkId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, bookmark.getCategory().getUser());

        bookmarkRepository.delete(bookmark);

        return new DeleteBookmarkRes();
    }

    /**
     * 즐겨찾기 수정 (memo 수정만 가능)
     */
    @Transactional
    public ModifyBookmarkRes modifyBookmark(Long userId, Long categoryId, Long bookmarkId, ModifyBookmarkReq req) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);
        Bookmark bookmark = findBookmark(category, bookmarkId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, bookmark.getCategory().getUser());

        bookmark.update(req.getMemo());

        return new ModifyBookmarkRes();
    }

    /**
     * 즐겨찾기 조회
     */
    @Transactional(readOnly = true)
    public GetBookmarkRes getBookmark(Long userId, Long categoryId, Long bookmarkId) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);
        Bookmark bookmark = findBookmark(category, bookmarkId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, bookmark.getCategory().getUser());

        return new GetBookmarkRes(bookmark);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() -> new GlobalException(NOT_FOUND_CATEGORY));
    }

    private void checkAuthority(User accessor, User owner) {
        if(!accessor.getUserId().equals(owner.getUserId())) {
            throw new GlobalException(UNAUTHORIZED);
        }
    }

    private Bookmark findBookmark(Category category, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId).orElseThrow(() -> new GlobalException(NOT_FOUND_BOOKMARK));

        if(category.equals(bookmark.getCategory())) {
            return bookmark;
        }
        throw new GlobalException(NOT_FOUND_IN_CATEGORY);
    }

    private void checkPlaceDuplication(Category category, PlaceType placeType, Long placeId) {
        Optional<Bookmark> optionalBookmark = bookmarkRepository.findByCategoryAndPlaceTypeAndPlaceId(category, placeType, placeId);

        // 같은 카테고리에 동일 북마크가 존재하는 경우
        if (optionalBookmark.isPresent()) {
            throw new GlobalException(DUPLICATED_BOOKMARK);
        }
    }

    public void checkPlaceExists(PlaceType placeType, Long placeId) {
        if (PlaceType.BUILDING.equals(placeType) && !buildingRepository.existsById(placeId)) {
            throw new GlobalException(NOT_FOUND_BUILDING);
        } else if (PlaceType.CLASSROOM.equals(placeType) && !classroomRepository.existsById(placeId)) {
            throw new GlobalException(NOT_FOUND_CLASSROOM);
        } else if (PlaceType.FACILITY.equals(placeType) && !facilityRepository.existsById(placeId)) {
            throw new GlobalException(NOT_FOUND_FACILITY);
        }
    }

}
