package devkor.com.teamcback.domain.place.entity;

import devkor.com.teamcback.domain.place.dto.request.CreatePlaceReq;
import devkor.com.teamcback.domain.place.dto.request.ModifyPlaceReq;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.routes.entity.Node;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "tb_place")
@NoArgsConstructor
public class Place extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PlaceType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double floor;

    @Column(nullable = false) // 장소 다른 이름
    private String detail;

    @Column(nullable = false, length = 500) // 상세 정보
    private String description = "";

    @Column(nullable = false)
    private boolean availability;

    @Column(nullable = false)
    private boolean plugAvailability;

    private String imageUrl;

    private String fileUuid;

    @Setter
    private String operatingTime;

    @Setter
    private String weekdayOperatingTime;

    @Setter
    private String saturdayOperatingTime;

    @Setter
    private String sundayOperatingTime;

    @Setter
    @Column(nullable = false)
    private boolean isOperating;

    private Integer maskIndex;

    @Column(nullable = false)
    private Integer starSum = 0;

    @Column(nullable = false)
    private Integer starNum = 0;

    @Column
    private String contact; // 연락처 등

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private Node node;

    public Place(CreatePlaceReq req, Building building, Node node) {
        this.type = req.getType();
        this.name = req.getName();
        this.floor = (double) req.getFloor();
        this.detail = req.getDetail();
        this.availability = req.isAvailability();
        this.plugAvailability = req.isPlugAvailability();
        this.imageUrl = req.getImageUrl();
        this.operatingTime = req.getOperatingTime();
        this.isOperating = req.isOperating();
        this.maskIndex = req.getMaskIndex();
        this.building = building;
        this.node = node;
        this.description = req.getDescription();
    }

    public Place(PlaceType type, Building building) {
        this.type = type;
        this.name = type.getName();
        this.building = building;
    }

    public void update(ModifyPlaceReq req, Building building, Node node) {
        this.type = req.getType();
        this.name = req.getName();
        this.floor = (double) req.getFloor();
        this.detail = req.getDetail();
        this.availability = req.isAvailability();
        this.plugAvailability = req.isPlugAvailability();
        this.imageUrl = req.getImageUrl();
        this.operatingTime = req.getOperatingTime();
        this.isOperating = req.isOperating();
        this.maskIndex = req.getMaskIndex();
        this.building = building;
        this.node = node;
        this.description = req.getDescription();
    }
}
