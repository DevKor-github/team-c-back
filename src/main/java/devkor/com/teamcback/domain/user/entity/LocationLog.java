package devkor.com.teamcback.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "tb_location_log")
@NoArgsConstructor
public class LocationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = LocationEncryptionConverter.class)
    private Double latitude;    // 위도
    @Convert(converter = LocationEncryptionConverter.class)
    private Double longitude;   // 경도
    @Convert(converter = LocationEncryptionConverter.class)
    private Double altitude;    // 고도

    private String collectionMethod;    // 취득경로
    private String usagePurpose;        // 이용목적 (예: 주변 상점 검색)

    @Column(updatable = false)
    private LocalDateTime createdAt; // 수집 및 이용 일시
}
