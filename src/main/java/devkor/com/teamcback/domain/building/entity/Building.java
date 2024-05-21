package devkor.com.teamcback.domain.building.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
    private Double longitude;

    @Column(nullable = false)
    private Double latitude;

    public Building(String name, String imageUrl, String detail, String address, String operatingTime,
        Boolean needStudentCard, Double longitude, Double latitude) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.detail = detail;
        this.address = address;
        this.operatingTime = operatingTime;
        this.needStudentCard = needStudentCard;
        this.longitude = longitude;
        this.latitude = latitude;
    }
}
