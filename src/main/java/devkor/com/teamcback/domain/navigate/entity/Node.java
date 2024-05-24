package devkor.com.teamcback.domain.navigate.entity;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
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
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private double xCoord;

    @Column(nullable = false)
    private double yCoord;

    @Column(nullable = false)
    private int floor;

    @Column(nullable = false)
    private boolean routing;

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;
}
