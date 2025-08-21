package devkor.com.teamcback.domain.building.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
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
@Table(name = "tb_building_nickname")
@NoArgsConstructor
public class BuildingNickname extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(nullable = false)
    private String nickname;

    @Column()
    private String chosung;

    @Column()
    private String jasoDecompose;

    public BuildingNickname(Building building, String nickname, String chosung, String jasoDecompose) {
        this.building = building;
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
