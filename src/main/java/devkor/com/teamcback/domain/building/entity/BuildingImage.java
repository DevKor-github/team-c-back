package devkor.com.teamcback.domain.building.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Table(name = "tb_building_image")
@NoArgsConstructor
public class BuildingImage {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private Double floor;

    @Column(nullable = false) // 추후 삭제
    private String image;

    @Setter
    @Column
    private String fileUuid;

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    public BuildingImage(Double floor, String imageUrl, Building building) {
        this.floor = floor;
        this.image = imageUrl;
        this.building = building;
    }

    public BuildingImage(Double floor, Building building, String fileUuid) {
        this.floor = floor;
        this.fileUuid = fileUuid;
        this.building = building;
    }

    public void update(String imageUrl) {
        this.image = imageUrl;
    }

}
