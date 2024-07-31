package devkor.com.teamcback.domain.facility.entity;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.navigate.entity.Node;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    public void setOperating(boolean operating) {
        isOperating = operating;
    }
}
