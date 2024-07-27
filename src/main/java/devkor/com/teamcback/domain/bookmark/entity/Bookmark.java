package devkor.com.teamcback.domain.bookmark.entity;

import devkor.com.teamcback.domain.bookmark.dto.request.CreateBookmarkReq;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.facility.entity.Facility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_bookmark")
@NoArgsConstructor
public class Bookmark {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaceType placeType;

    @Column(nullable = false)
    private Long placeId;

    @Column
    private String memo;

//    @ManyToOne
//    @JoinColumn(name = "building_id")
//    private Building building;
//
//    @ManyToOne
//    @JoinColumn(name = "classroom_id")
//    private Classroom classroom;
//
//    @ManyToOne
//    @JoinColumn(name = "facility_id")
//    private Facility facility;

    public Bookmark(CreateBookmarkReq req, Category category) {
        //category_id, memo, placeType, 빌교편중1
        this.category = category;
        this.memo = req.getMemo();
        this.placeType = req.getPlaceType();
        this.placeId = req.getPlaceId();

    }

    public void update(String memo) {
        this.memo = memo;
    }

}
