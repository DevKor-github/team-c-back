package devkor.com.teamcback.domain.bookmark.repository;

import devkor.com.teamcback.TestTimer;
import devkor.com.teamcback.domain.bookmark.dto.request.CreateBookmarkReq;
import devkor.com.teamcback.domain.bookmark.entity.Bookmark;
import devkor.com.teamcback.domain.bookmark.entity.Category;
import devkor.com.teamcback.domain.bookmark.entity.CategoryBookmark;
import devkor.com.teamcback.domain.bookmark.entity.Color;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookmarkRepositoryTest {
    @Autowired
    BookmarkRepository bookmarkRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    CategoryBookmarkRepository categoryBookmarkRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager em;

    Category targetCategory;
    Bookmark targetBookmark;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("user1", "email", Role.USER, Provider.KAKAO));
        targetBookmark = bookmarkRepository.save(new Bookmark(new CreateBookmarkReq(new ArrayList<>(), LocationType.PLACE, 1L, "memo")));
        targetCategory = categoryRepository.save(new Category("category1", Color.RED, user));

        CategoryBookmark cb = new CategoryBookmark();
        cb.setCategoryAndBookmark(targetCategory, targetBookmark);
        categoryBookmarkRepository.save(cb);

        for (int i = 0; i < 3; i++) {
            Category category = categoryRepository.save(new Category("category1", Color.RED, user));
            CategoryBookmark cb1 = new CategoryBookmark();
            cb1.setCategoryAndBookmark(category, targetBookmark);
            categoryBookmarkRepository.save(cb1);
        }

        for (int i = 0; i < 3; i++) {
            Bookmark bookmark = bookmarkRepository.save(new Bookmark(new CreateBookmarkReq(new ArrayList<>(), LocationType.PLACE, (long)i+4, "memo")));
            CategoryBookmark cb1 = new CategoryBookmark();
            cb1.setCategoryAndBookmark(targetCategory, bookmark);
            categoryBookmarkRepository.save(cb1);
        }

        for (int i = 0; i < 10000; i++) {
            Category category = categoryRepository.save(new Category("category1", Color.RED, user));
            Bookmark bookmark = bookmarkRepository.save(new Bookmark(new CreateBookmarkReq(new ArrayList<>(), LocationType.PLACE, (long)i + 100, "memo")));

            CategoryBookmark cb1 = new CategoryBookmark();
            cb1.setCategoryAndBookmark(category, bookmark);
            categoryBookmarkRepository.save(cb1);
        }

        em.flush();
        em.clear();
    }

//    @Test
    @DisplayName("카테고리 북마크 확인")
    void findByCategoryAndBookmark() {

        TestTimer.run("findByCategoryAndBookmark", () -> {
            categoryBookmarkRepository.findByCategoryAndBookmark(targetCategory, targetBookmark);
        });

        // 36ms
    }

//    @Test
    @DisplayName("카테고리 북마크 확인 - 인덱스(카테고리, 북마크) 추가")
    void findByCategoryAndBookmarkWithIndex1() {
        em.createNativeQuery("CREATE INDEX idx1 ON tb_category_bookmark (category_id, bookmark_id)").executeUpdate();

        em.flush();
        em.clear();

        TestTimer.run("findByCategoryAndBookmarkWithIndex1", () -> {
            categoryBookmarkRepository.findByCategoryAndBookmark(targetCategory, targetBookmark);
        });

        // 399ms - 59ms
    }

//    @Test
    @DisplayName("카테고리 북마크 확인 - 인덱스(카테고리), 인덱스(북마크) 추가")
    void findByCategoryAndBookmarkWithIndex2() {
        em.createNativeQuery("CREATE INDEX idx1 ON tb_category_bookmark (category_id)").executeUpdate();
        em.createNativeQuery("CREATE INDEX idx2 ON tb_category_bookmark (bookmark_id)").executeUpdate();

        em.flush();
        em.clear();

        TestTimer.run("findByCategoryAndBookmarkWithIndex2", () -> {
            categoryBookmarkRepository.findByCategoryAndBookmark(targetCategory, targetBookmark);
        });

        // 407ms - 45ms
    }

//    @Test
    @DisplayName("북마크를 카테고리와 장소로 확인")
    void findByCategoryAndLocationTypeAndLocationId() {
        TestTimer.run("findByCategoryAndLocationTypeAndLocationId", () -> {
            bookmarkRepository.findByLocationIdAndLocationTypeAndCategoryBookmarkList_Category(targetBookmark.getLocationId(), targetBookmark.getLocationType(), targetCategory);
        });

        // 43ms
    }

//    @Test
    @DisplayName("북마크를 카테고리와 장소로 확인 - 북마크 인덱스(장소id, 장소type) 추가")
    void findByCategoryAndLocationTypeAndLocationIdWithIndex1() {
        em.createNativeQuery("CREATE INDEX idx1 ON tb_bookmark (locationId, locationType)").executeUpdate();

        em.flush();
        em.clear();

        TestTimer.run("findByCategoryAndLocationTypeAndLocationIdWithIndex1", () -> {
            bookmarkRepository.findByLocationIdAndLocationTypeAndCategoryBookmarkList_Category(targetBookmark.getLocationId(), targetBookmark.getLocationType(), targetCategory);
        });

        // 355ms - 72ms
    }

//    @Test
    @DisplayName("북마크를 카테고리와 장소로 확인 - 북마크 인덱스(장소id), 인덱스(장소type) 추가 -> 현재 방식")
    void findByCategoryAndLocationTypeAndLocationIdWithIndex2() {
        em.createNativeQuery("CREATE INDEX idx1 ON tb_bookmark (locationId)").executeUpdate();
        em.createNativeQuery("CREATE INDEX idx2 ON tb_bookmark (locationType)").executeUpdate();

        em.flush();
        em.clear();

        TestTimer.run("findByCategoryAndLocationTypeAndLocationIdWithIndex1", () -> {
            bookmarkRepository.findByLocationIdAndLocationTypeAndCategoryBookmarkList_Category(targetBookmark.getLocationId(), targetBookmark.getLocationType(), targetCategory);
        });

        // 409ms - 48ms
    }

//    @Test
    @DisplayName("북마크 조회")
    void findBookmark() {

        TestTimer.run("findBookmark", () -> {
            bookmarkRepository.findById(1L);
        });

    }

//    @Test
    @DisplayName("카테고리 조회")
    void findCategory() {

        TestTimer.run("findCategory", () -> {
            categoryRepository.findById(1L);
        });

    }

//    @Test
    @DisplayName("카테고리에 있는 북마크 리스트 조회")
    void findCategoryListByBookmark() {

        TestTimer.run("findCategoryListByBookmark", () -> {
            bookmarkRepository.findAllByCategoryBookmarkList_CategoryId(targetCategory.getId());
        });

        // 225ms - 88ms
        // left join category_bookmark left join category

        // -> @Qeury로 수정 후

        // 172ms - 57ms
        // join
    }

//    @Test
    @DisplayName("카테고리 리스트에 속하는 장소로 북마크 조회")
    void findBookmarkByCategoryListAndLocation() {

        List<Category> categoryList = new ArrayList<>();
        categoryList.add(targetCategory);

        TestTimer.run("findBookmarkByCategoryListAndLocation", () -> {
            bookmarkRepository.findByLocationIdAndLocationTypeAndCategoryBookmarkList_CategoryIn(targetBookmark.getLocationId(), targetBookmark.getLocationType(), categoryList);
        });

        // 157ms - 53ms
        // left join category_bookmark left join category

        // 153ms - 52ms
        // join category_bookmark
    }
}