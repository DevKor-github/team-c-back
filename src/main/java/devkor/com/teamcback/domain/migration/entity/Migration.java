package devkor.com.teamcback.domain.migration.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "tb_migration")
public class Migration {
    @Id
    @Schema(description = "id", example = "1")
    private Long id;
    @Schema(description = "isMigrating", example = "true")
    private Boolean isMigrating;
}
