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

    @Column()
    private String chosung;

    @Column()
    private String jasoDecompose;

    public PlaceNickname(Place place, String nickname, String chosung, String jasoDecompose) {
        this.place = place;
        this.nickname = nickname;
        this.chosung = chosung;
        this.jasoDecompose = jasoDecompose;
    }

    public void update(String chosung, String jasoDecompose) {
        this.chosung = chosung;
        this.jasoDecompose = jasoDecompose;
    }

    public void update(String nickname, String chosung, String jasoDecompose) {
        this.nickname = nickname;
        this.chosung = chosung;
        this.jasoDecompose = jasoDecompose;
    }
}
