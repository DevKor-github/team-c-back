package devkor.com.teamcback.domain.facility.entity;

import devkor.com.teamcback.domain.admin.facility.dto.request.CreateFacilityReq;
import devkor.com.teamcback.domain.admin.facility.dto.request.ModifyFacilityReq;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.navigate.entity.Node;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_facility")
@NoArgsConstructor
public class Facility extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FacilityType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double floor;

    @Column(nullable = false)
    private String detail;

    @Column(nullable = false)
    private boolean availability;

    @Column(nullable = false)
    private boolean plugAvailability;

    private String imageUrl;

    private String operatingTime;

    private boolean isOperating;

    private Integer maskIndex;

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    @OneToOne
    @JoinColumn(name = "node_id")
    private Node node;

    public Facility(CreateFacilityReq req, Building building, Node node) {
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
    }

    public void update(ModifyFacilityReq req, Building building, Node node) {
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
    }

    public void setOperating(boolean operating) {
        isOperating = operating;
    }

    public void setOperatingTime(String operatingTime) {
        String remainingString = "";
        if(this.operatingTime.length() > 11) remainingString = this.operatingTime.substring(11);
        this.operatingTime = operatingTime + remainingString;
    }
}
