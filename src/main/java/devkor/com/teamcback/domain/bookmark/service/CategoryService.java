package devkor.com.teamcback.domain.bookmark.service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_CATEGORY;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;
import static devkor.com.teamcback.global.response.ResultCode.UNAUTHORIZED;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateCategoryReq;
import devkor.com.teamcback.domain.bookmark.dto.response.CreateCategoryRes;
import devkor.com.teamcback.domain.bookmark.dto.response.DeleteCategoryRes;
import devkor.com.teamcback.domain.bookmark.dto.response.GetCategoryRes;
import devkor.com.teamcback.domain.bookmark.dto.response.ModifyCategoryRes;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.repository.CategoryRepository;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * 카테고리 생성
     */
    @Transactional
    public CreateCategoryRes createCategory(Long userId, CreateCategoryReq req) {
        User user = findUser(userId);

        categoryRepository.save(new Category(req, user));

        return new CreateCategoryRes();
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
    public ModifyCategoryRes modifyCategory(Long userId, Long categoryId, CreateCategoryReq req) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, category.getUser());

        category.update(req);

        return new ModifyCategoryRes();
    }

    @Transactional(readOnly = true)
    public GetCategoryRes getCategory(Long userId, Long categoryId) {
        User user = findUser(userId);
        Category category = findCategory(categoryId);

        // 카테고리 소유자인지 확인
        checkAuthority(user, category.getUser());

        return new GetCategoryRes(category);
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
}
