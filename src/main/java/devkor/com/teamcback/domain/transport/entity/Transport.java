package devkor.com.teamcback.domain.transport.entity;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_transport")
@NoArgsConstructor
public class Transport extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransportType type;

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;
}
