package devkor.com.teamcback.domain.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_version")
@NoArgsConstructor
public class Version {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private String version;

    public Version(String version) {
        this.id = 1L;
        this.version = version;
    }

    public void update(String version) {
        this.version = version;
    }
}
