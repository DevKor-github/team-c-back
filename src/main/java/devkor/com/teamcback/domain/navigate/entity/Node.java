package devkor.com.teamcback.domain.navigate.entity;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.navigate.dto.request.CreateNodeReq;
import devkor.com.teamcback.domain.navigate.dto.request.ModifyNodeReq;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_node")
@NoArgsConstructor
public class Node extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NodeType type;

    private Double xCoord;

    private Double yCoord;

    private Double latitude;

    private Double longitude;

    private Double floor;

    @Column(nullable = false)
    private boolean routing;

    @Column(nullable = false)
    private boolean isOperating = true;

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    private String adjacentNode;

    private String distance;

    public Node(Building building, CreateNodeReq req) {
        this.type = req.getType();
        this.xCoord = req.getXCoord();
        this.yCoord = req.getYCoord();
        this.latitude = req.getLatitude();
        this.longitude = req.getLongitude();
        this.floor = req.getFloor();
        this.routing = req.isRouting();
        this.building = building;
        this.adjacentNode = req.getAdjacentNode();
        this.distance = req.getDistance();
    }

    public void update(Building building, ModifyNodeReq req) {
        this.type = req.getType();
        this.xCoord = req.getXCoord();
        this.yCoord = req.getYCoord();
        this.latitude = req.getLatitude();
        this.longitude = req.getLongitude();
        this.floor = req.getFloor();
        this.routing = req.isRouting();
        this.building = building;
        this.adjacentNode = req.getAdjacentNode();
        this.distance = req.getDistance();
    }

    public void setOperating(boolean operating) {
        isOperating = operating;
    }
}
