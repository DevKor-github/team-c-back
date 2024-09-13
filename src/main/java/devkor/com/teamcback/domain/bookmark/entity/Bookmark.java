package devkor.com.teamcback.domain.bookmark.entity;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateBookmarkReq;
import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.common.LocationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_bookmark")
@NoArgsConstructor
public class Bookmark extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType locationType;

    @Column(nullable = false)
    private Long locationId;

    @Column
    private String memo;

    public Bookmark(CreateBookmarkReq req, Category category) {
        //category_id, memo, placeType, 빌교편중1
        this.category = category;
        this.memo = req.getMemo();
        this.locationType = req.getLocationType();
        this.locationId = req.getLocationId();

    }

    public void update(String memo) {
        this.memo = memo;
    }

}
