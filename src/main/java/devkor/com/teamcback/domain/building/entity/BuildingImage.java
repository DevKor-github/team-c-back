package devkor.com.teamcback.domain.building.entity;

import devkor.com.teamcback.domain.admin.dto.request.SaveBuildingImageReq;
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
@Table(name = "tb_building_image")
@NoArgsConstructor
public class BuildingImage {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private Double floor;

    @Column(nullable = false)
    private String image;

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    public BuildingImage(SaveBuildingImageReq req, String fileUrl, Building building) {
        this.floor = req.getFloor();
        this.image = fileUrl;
        this.building = building;
    }
}
