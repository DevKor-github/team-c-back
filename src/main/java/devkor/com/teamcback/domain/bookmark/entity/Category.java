package devkor.com.teamcback.domain.bookmark.entity;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateCategoryReq;
import devkor.com.teamcback.domain.bookmark.dto.request.ModifyCategoryReq;
import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_category")
@NoArgsConstructor
public class Category extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category; // 카테고리명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Color color;

    @Column
    private String memo;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "category", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<CategoryBookmark> categoryBookmarkList = new ArrayList<>();

    public Category(CreateCategoryReq req, User user) {
        this.category = req.getCategory();
        this.color = req.getColor();
        this.memo = req.getMemo();
        this.user = user;
    }

    public void update(ModifyCategoryReq req) {
        this.category = req.getCategory();
        this.color = req.getColor();
        this.memo = req.getMemo();
    }
}
