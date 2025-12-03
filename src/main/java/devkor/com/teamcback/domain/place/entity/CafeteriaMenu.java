package devkor.com.teamcback.domain.place.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "tb_cafeteria_menu")
@NoArgsConstructor
public class CafeteriaMenu extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false) // 식단 구분 - 조식/중식/석식
    private String kind;

    @Getter
    @Setter
    @Column(nullable = false, length = 500) // 식단 내용
    private String menu;

    @Column(nullable = false) // 상세 정보
    private Long placeId;

    public CafeteriaMenu(LocalDate date, String kind, String menu, Long placeId) {
        this.date = date;
        this.kind = kind;
        this.menu = menu;
        this.placeId = placeId;
    }

    @Override
    public String toString() {
        return "CafeteriaMenu{" +
                "id=" + id +
                ", date=" + date +
                ", kind='" + kind + '\'' +
                ", menu='" + menu + '\'' +
                ", placeId=" + placeId +
                '}';
    }
}

