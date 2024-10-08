package devkor.com.teamcback.domain.koyeon.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "tb_koyeon")
public class Koyeon {
    @Id
    @Schema(description = "id", example = "1")
    private Long id;
    @Schema(description = "isKoyeon", example = "true")
    private Boolean isKoyeon;
}
