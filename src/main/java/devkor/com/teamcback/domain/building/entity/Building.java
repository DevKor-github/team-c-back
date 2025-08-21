package devkor.com.teamcback.domain.building.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.routes.entity.Node;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private String imageUrl; // 추후 삭제

    private String fileUuid;

    @Column(nullable = false)
    private String detail;

    @Column(nullable = false)
    private String address;

    @Setter
    private String operatingTime; // 오늘의 운영 시간

    private String weekdayOperatingTime;
    private String saturdayOperatingTime;
    private String sundayOperatingTime;

    private boolean isOperating = true;

    @Column(nullable = false)
    private boolean needStudentCard;

    @Column(nullable = false)
    private Double floor; // 건물 최대 층수

    @Column(nullable = false)
    private Double underFloor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private Node node;

    public void setOperating(boolean operating) {
        this.isOperating = operating;
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
