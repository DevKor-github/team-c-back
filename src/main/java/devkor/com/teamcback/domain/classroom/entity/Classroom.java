package devkor.com.teamcback.domain.classroom.entity;

import devkor.com.teamcback.domain.admin.classroom.dto.request.CreateClassroomReq;
import devkor.com.teamcback.domain.admin.classroom.dto.request.ModifyClassroomReq;
import devkor.com.teamcback.domain.admin.classroom.dto.response.CreateClassroomRes;
import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.navigate.entity.Node;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_classroom")
@NoArgsConstructor
public class Classroom extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String detail;

    @Column(nullable = false)
    private boolean plugAvailability;

    private String imageUrl;

    @Column(nullable = false)
    private Double floor;

    private Integer maskIndex;

    private String operatingTime;

    @Column(nullable = false)
    private boolean isOperating = true;

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    @OneToOne
    @JoinColumn(name = "node_id")
    private Node node;

    public Classroom(CreateClassroomReq req, Building building, Node node) {
        this.name = req.getName();
        this.detail = req.getDetail();
        this.plugAvailability = req.isPlugAvailability();
        this.imageUrl = req.getImageUrl();
        this.floor = (double) req.getFloor();
        this.maskIndex = req.getMaskIndex();
        this.operatingTime = req.getOperatingTime();
        this.isOperating = req.isOperating();
        this.building = building;
        this.node = node;
    }

    public void update(ModifyClassroomReq req, Building building, Node node) {
        this.name = req.getName();
        this.detail = req.getDetail();
        this.plugAvailability = req.isPlugAvailability();
        this.imageUrl = req.getImageUrl();
        this.floor = (double) req.getFloor();
        this.maskIndex = req.getMaskIndex();
        this.operatingTime = req.getOperatingTime();
        this.isOperating = req.isOperating();
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
