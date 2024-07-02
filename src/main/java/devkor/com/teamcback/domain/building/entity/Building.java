package devkor.com.teamcback.domain.building.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.navigate.entity.Node;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_building")
@NoArgsConstructor
public class Building extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    private String imageUrl;

    @Column(nullable = false)
    private String detail;

    @Column(nullable = false)
    private String address;

    private String operatingTime;

    @Column(nullable = false)
    private Boolean needStudentCard;

    @Column(nullable = false)
    private Integer floor; // 건물 최대 층수

    @Column(nullable = false)
    private Integer underFloor;

    @OneToOne
    @JoinColumn(name = "node_id")
    private Node node;

    public Building(String name, String imageUrl, String detail, String address, String operatingTime,
        Boolean needStudentCard) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.detail = detail;
        this.address = address;
        this.operatingTime = operatingTime;
        this.needStudentCard = needStudentCard;
    }
}
