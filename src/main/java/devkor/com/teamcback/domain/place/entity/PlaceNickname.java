package devkor.com.teamcback.domain.place.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_place_nickname")
@NoArgsConstructor
public class PlaceNickname extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(nullable = false)
    private String nickname;

    public PlaceNickname(Place place, String nickname) {
        this.place = place;
        this.nickname = nickname;
    }
}
