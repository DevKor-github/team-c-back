package devkor.com.teamcback.domain.bookmark.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostRemove;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor
@Table(name = "tb_category_bookmark")
public class CategoryBookmark extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmark_id")
    private Bookmark bookmark;

    public void setCategoryAndBookmark(Category category, Bookmark bookmark) {
        addCategoryBookmarkForCategory(category);
        addCategoryBookmarkForBookmark(bookmark);
        setCategory(category);
        setBookmark(bookmark);
    }

    private void addCategoryBookmarkForCategory(Category category) {
        if(!category.getCategoryBookmarkList().contains(this)) {
            category.getCategoryBookmarkList().add(this);
        }
    }

    private void addCategoryBookmarkForBookmark(Bookmark bookmark) {
        if(!bookmark.getCategoryBookmarkList().contains(this)) {
            bookmark.getCategoryBookmarkList().add(this);
        }
    }

    private void setCategory(Category category) {
        this.category = category;
    }

    private void setBookmark(Bookmark bookmark) {
        this.bookmark = bookmark;
    }

    public void remove() {
        removeFromCategory();
        removeFromBookmark();
    }

    private void removeFromCategory() {
        if(this.category != null) {
            category.getCategoryBookmarkList().remove(this);
        }
    }

    private void removeFromBookmark() {
        if(this.bookmark != null) {
            bookmark.getCategoryBookmarkList().remove(this);
        }
    }
}
