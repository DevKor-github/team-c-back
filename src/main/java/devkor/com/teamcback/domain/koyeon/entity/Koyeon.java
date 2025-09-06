package devkor.com.teamcback.domain.koyeon.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_koyeon")
@NoArgsConstructor
public class Koyeon {
    @Id
    @Schema(description = "id", example = "1")
    private Long id;
    @Schema(description = "name", example = "고연전")
    private String name;
    @Schema(description = "isKoyeon", example = "true")
    private Boolean isKoyeon;

    public Koyeon(Long id, String name, Boolean isKoyeon) {
        this.id = id;
        this.name = name;
        this.isKoyeon = isKoyeon;
    }
}
