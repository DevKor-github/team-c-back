package devkor.com.teamcback.domain.bookmark.service;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateCategoryReq;
import devkor.com.teamcback.domain.bookmark.dto.request.ModifyCategoryReq;
import devkor.com.teamcback.domain.bookmark.dto.response.*;
import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.entity.CategoryBookmark;
import devkor.com.teamcback.domain.bookmark.repository.BookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryBookmarkRepository;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CategoryBookmarkRepository categoryBookmarkRepository;

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

        // 각 북마크가 다른 카테고리와 연결되어 있지 않은지 확인 후 삭제
        category.getCategoryBookmarkList().forEach(categoryBookmark -> {
            Bookmark bookmark = categoryBookmark.getBookmark();
            if (bookmark.getCategoryBookmarkList().size() == 1) {
                bookmarkRepository.delete(bookmark);
            }
        });

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
    public GetCategoryListRes getAllCategories(Long userId, LocationType type, Long id) {
        User user = findUser(userId);

        List<Category> userCategoryList = categoryRepository.findAllByUser(user);

        if(type != null && id != null) {
            Bookmark bookmark = bookmarkRepository.findByLocationIdAndLocationTypeAndCategoryBookmarkList_CategoryIn(id, type, userCategoryList);
            if(bookmark != null) {
                List<Category> categories = bookmark.getCategoryBookmarkList().stream().map(CategoryBookmark::getCategory).toList();
                // 사용자의 카테고리 중 즐겨찾기의 카테고리가 포함되는지 확인
                GetCategoryListRes getCategoryListRes = new GetCategoryListRes(userCategoryList
                    .stream().map(category -> new GetCategoryRes(category, category.getCategoryBookmarkList().size(), categories.contains(category))).toList());
                getCategoryListRes.setBookmarkId(bookmark.getId());
                return getCategoryListRes;
            }
        }

        return new GetCategoryListRes(userCategoryList
                .stream().map(category -> new GetCategoryRes(category, category.getCategoryBookmarkList().size())).toList());

    }

    /**
     * 특정 카테고리 상세조회 (즐겨찾기 List)
     */
    @Transactional(readOnly = true)
    public GetBookmarkListRes getAllBookmarks(Long userId, Long categoryId) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, category.getUser());

        return new GetBookmarkListRes(bookmarkRepository.findAllByCategoryBookmarkList_CategoryId(categoryId).stream().map(GetBookmarkRes::new).toList());
    }

    /**
     * 카테고리 내 즐겨찾기 상세 조회
     */
    @Transactional(readOnly = true)
    public GetBookmarkRes getBookmark(Long userId, Long categoryId, Long bookmarkId) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);
        Bookmark bookmark = findBookmark(category, bookmarkId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, category.getUser());

        return new GetBookmarkRes(bookmark);
    }

    /**
     * 카테고리 내 즐겨찾기 삭제
     */
    @Transactional
    public DeleteBookmarkRes deleteBookmark(Long userId, Long categoryId, Long bookmarkId) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);

        //해당 카테고리에 해당 북마크가 존재하는지 확인 후 가져오기
        Bookmark bookmark = findBookmark(category, bookmarkId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, category.getUser());

        CategoryBookmark categoryBookmark = findCategoryBookmark(category, bookmark);
        deleteCategoryBookmark(categoryBookmark);
        return new DeleteBookmarkRes();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() -> new GlobalException(NOT_FOUND_CATEGORY));
    }

    private void checkAuthority(User accessor, User owner) {
        if(!accessor.getUserId().equals(owner.getUserId())) {
            throw new GlobalException(FORBIDDEN);
        }
    }

    private Bookmark findBookmark(Category category, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId).orElseThrow(() -> new GlobalException(NOT_FOUND_BOOKMARK));

        if(!categoryBookmarkRepository.existsByCategoryAndBookmark(category, bookmark)) {
            throw new GlobalException(NOT_FOUND_IN_CATEGORY);
        }

        return bookmark;
    }

    private CategoryBookmark findCategoryBookmark(Category category, Bookmark bookmark) {
        return categoryBookmarkRepository.findByCategoryAndBookmark(category, bookmark).orElseThrow(() -> new GlobalException(NOT_FOUND_IN_CATEGORY));
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
