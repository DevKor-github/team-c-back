package devkor.com.teamcback.domain.transport.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.navigate.entity.Node;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.CustomLog;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_transport_floor")
@NoArgsConstructor
public class TransportFloor extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private int floor;

    @ManyToOne
    @JoinColumn(name = "transport_id")
    private Transport transport;

    @OneToOne
    @JoinColumn(name = "node_id")
    private Node node;
}
