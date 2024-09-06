package devkor.com.teamcback.domain.search.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@Table(name = "tb_koyeon")
public class Koyeon {
    @Id
    private Long id;

    private Boolean isKoyeon;
}
