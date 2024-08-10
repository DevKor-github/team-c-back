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
import java.util.Objects;
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

    private boolean isOperating = true;

    @Column(nullable = false)
    private boolean needStudentCard;

    @Column(nullable = false)
    private Double floor; // 건물 최대 층수

    @Column(nullable = false)
    private Double underFloor;

    @OneToOne
    @JoinColumn(name = "node_id")
    private Node node;

    public void setOperating(boolean operating) {
        isOperating = operating;
    }

    public void setOperatingTime(String operatingTime) {
        this.operatingTime = operatingTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Building building = (Building) o;
        return Objects.equals(id, building.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
