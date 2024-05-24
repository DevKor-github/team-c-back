package devkor.com.teamcback.domain.navigate.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_edge")
@NoArgsConstructor
public class Edge extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private int distance;

    @ManyToOne
    @JoinColumn(name = "start_node_id", nullable = false)
    private Node startNode;

    @ManyToOne
    @JoinColumn(name = "end_node_id", nullable = false)
    private Node endNode;
}
