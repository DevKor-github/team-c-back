package devkor.com.teamcback.domain.classroom.entity;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.navigate.entity.Node;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
    private int floor;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double latitude;

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    @OneToOne
    @JoinColumn(name = "node_id")
    private Node node;

    public Classroom(String name, String detail, boolean plugAvailability, String imageUrl,
        int floor, double longitude, double latitude, Building building) {
        this.name = name;
        this.detail = detail;
        this.plugAvailability = plugAvailability;
        this.imageUrl = imageUrl;
        this.floor = floor;
        this.longitude = longitude;
        this.latitude = latitude;
        this.building = building;
    }
}
