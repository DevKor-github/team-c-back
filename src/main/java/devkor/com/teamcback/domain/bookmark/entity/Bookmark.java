package devkor.com.teamcback.domain.bookmark.entity;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateBookmarkReq;
import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.common.LocationType;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_bookmark")
@NoArgsConstructor
public class Bookmark extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType locationType;

    @Column(nullable = false)
    private Long locationId;

    @Column
    private String memo;

    @OneToMany(mappedBy = "bookmark", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<CategoryBookmark> categoryBookmarkList = new ArrayList<>();

    public Bookmark(CreateBookmarkReq req) {
        this.memo = req.getMemo();
        this.locationType = req.getLocationType();
        this.locationId = req.getLocationId();
    }

    public void update(String memo) {
        this.memo = memo;
    }

}
