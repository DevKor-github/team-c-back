package devkor.com.teamcback.domain.place.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "tb_place_image")
@NoArgsConstructor
public class PlaceImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String image;   // 추후 삭제

    @Column(nullable = false)
    private String fileUuid;

    @ManyToOne
    @JoinColumn(name = "place_id")
    private Place place;
}
